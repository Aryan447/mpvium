# Security Policy

## Supported Versions

mpvium is under active development. Security fixes are provided for the
latest release and preview builds.

| Version | Supported          |
| ------- | ------------------ |
| Latest `v*` release | :white_check_mark: |
| Latest `v*-preview*` | :white_check_mark: |
| Older releases | :x: (please upgrade and re-test) |

User-facing fixes are tracked in [`CHANGELOG.md`](CHANGELOG.md).

## Reporting a Vulnerability

**Do not open a public issue for security vulnerabilities.**

Instead, use GitHub's private reporting:

1. Go to https://github.com/aryan447/mpvium/security/advisories/new
   (Security tab → Advisories → New draft advisory), or
2. Open a minimal public issue that says "possible security issue — please
   contact me" without technical details, and maintainers will follow up.

Include if possible:

- App version / commit SHA, APK flavor + ABI, Android version + device
- Steps to reproduce (without exploits that harm others)
- Impact assessment (what an attacker could / could not do)
- Logs, screenshots (redact private paths, tokens, media URLs)

We will acknowledge receipt promptly, investigate, and keep you updated.
Please give us reasonable time to fix before public disclosure.

## Scope notes

- Release signing uses GitHub Actions secrets. Never post keystores,
  passwords, tokens, or `local.properties` secrets in issues / PRs.
- Bundled native components (mpv, FFmpeg) follow upstream fixes; please
  note the upstream version if relevant.
- `NOTICE` lists third-party licenses — preserve attributions when fixing
  vendored code.

Thank you for helping keep mpvium and its users safe.
