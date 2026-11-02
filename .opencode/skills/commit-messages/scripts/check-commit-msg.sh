#!/bin/sh
# Validate an mpvium commit message against the rules in ../SKILL.md.
#
# Usage:
#   check-commit-msg.sh                     # check HEAD
#   check-commit-msg.sh .git/COMMIT_EDITMSG # check a file (commit-msg hook)
#   check-commit-msg.sh "feat(app): ..."    # check a string
#
# Exit codes: 0 ok (warnings allowed), 1 rule violations.

set -u

input=${1:-}
if [ -z "$input" ]; then
  msg=$(git log -1 --format=%B 2>/dev/null) || {
    echo "error: no commit message given and git log failed"
    exit 1
  }
elif [ -f "$input" ]; then
  msg=$(cat "$input")
else
  msg=$input
fi

# Drop comment lines and trailing blank lines (commit-msg hook input).
msg=$(printf '%s\n' "$msg" | sed '/^#/d' | awk 'NF { last = NR } { line[NR] = $0 } END { for (i = 1; i <= last; i++) print line[i] }')

subject=$(printf '%s\n' "$msg" | sed -n '1p')
body=$(printf '%s\n' "$msg" | sed -n '2,$p')

errors=0

err() {
  echo "error: $1"
  errors=$((errors + 1))
}

warn() {
  echo "warn:  $1"
}

if [ -z "$subject" ]; then
  err "empty commit subject"
  exit 1
fi

case "$subject" in
  *.) err "subject ends with a period" ;;
esac

len=$(printf '%s' "$subject" | wc -c | tr -d ' ')
if [ "$len" -gt 72 ]; then
  err "subject is $len characters (max 72)"
fi

case "$subject" in
  *': '*) : ;;
  *) err "expected 'type(scope): outcome' or 'type: outcome'" ;;
esac

type=$(printf '%s' "$subject" | sed -n 's/^\([A-Za-z]*\)(.*$/\1/p; s/^\([A-Za-z]*\):.*$/\1/p' | head -n1)
case "$type" in
  feat|fix|perf|refactor|build|chore|docs|test|revert|style|ci) : ;;
  '') : ;;
  *) err "unknown type '$type' (feat|fix|perf|refactor|build|chore|docs|test|revert|style|ci)" ;;
esac

after=$(printf '%s' "$subject" | sed -n 's/^[A-Za-z]*\(([^)]*)\)\{0,1\}: \(.*\)$/\2/p')
case "$after" in
  '') : ;;
  [a-z0-9]*) : ;;
  *) warn "subject should start lowercase after the colon" ;;
esac

# Vocabulary that obliges the author to show a number.
IMPROVE='faster|quicker|speed|slower|improv|optimi[sz]|reduc|cut|shrink|lower|boost|leaner|smaller|slimmer|smoother|fewer|cheaper'

if printf '%s' "$subject" | grep -Eqi "$IMPROVE"; then
  if ! printf '%s' "$subject" | grep -Eq '[0-9]'; then
    err "subject claims an improvement without a measured number (see SKILL.md)"
  fi
fi

if printf '%s' "$subject" | grep -Eq '^perf(\([^)]*\))?:'; then
  if ! printf '%s' "$subject" | grep -Eq '[0-9]'; then
    err "perf subjects must state the measured outcome (e.g. 'cut X from A to B')"
  fi
elif printf '%s' "$subject" | grep -Eq '^fix(\([^)]*\))?:'; then
  if ! printf '%s' "$subject" | grep -Eq '[0-9]'; then
    warn "fix subjects read better with the concrete outcome; add a number when you measured one"
  fi
fi

if [ -n "$(printf '%s' "$body" | tr -d '[:space:]')" ]; then
  if printf '%s' "$body" | grep -Eqi "$IMPROVE"; then
    if ! printf '%s' "$body" | grep -Eq '[0-9]'; then
      if ! printf '%s' "$body" | grep -Eqi 'unmeasured'; then
        err "body claims an improvement without numbers or an 'Unmeasured' note"
      fi
    fi
  fi

  if printf '%s' "$msg" | grep -Eq '[0-9]+[[:space:]]*%'; then
    if ! printf '%s' "$msg" | grep -Eqi '^(Impact|Method|Before|After)'; then
      warn "percentage present but no Impact/Method section to explain it"
    fi
  fi
fi

if [ "$errors" -gt 0 ]; then
  echo
  echo "$errors rule violation(s). See .opencode/skills/commit-messages/SKILL.md"
  exit 1
fi

echo "ok: $subject"
