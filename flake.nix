{
  description = "mpvium — sane defaults, pro-level control, built on libmpv";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    { self, nixpkgs }:
    let
      systems = [
        "x86_64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];

      mkPkgs =
        system:
        import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

      # Keep in sync with app/build.gradle.kts (compileSdk = 37) and with the
      # AGP version in gradle/libs.versions.toml. Coordinates are the
      # sdkmanager ones:
      #   platforms;android-37.0
      #   build-tools;36.0.0 (AGP 9.5.1's default; 37.0.0 also exists)
      androidSdkFor =
        pkgs:
        (pkgs.androidenv.composeAndroidPackages {
          platformVersions = [ "37.0" ];
          buildToolsVersions = [ "36.0.0" ];
          platformToolsVersion = "37.0.1";
          cmdLineToolsVersion = "23.0";
          includeEmulator = false;
          includeSystemImages = false;
          includeSources = false;
          includeNDK = false;
          includeCmake = false;
        }).androidsdk;

      mkDevShell =
        pkgs:
        let
          sdk = androidSdkFor pkgs;
          jdk = pkgs.jdk17;
          androidHome = "${sdk}/libexec/android-sdk";
        in
        pkgs.mkShell {
          packages = [
            sdk
            jdk
            pkgs.git
            pkgs.which
            pkgs.jq
            pkgs.curl
            pkgs.unzip
            pkgs.nixfmt-rfc-style
            pkgs.statix
            pkgs.deadnix
          ];

          ANDROID_HOME = androidHome;
          ANDROID_SDK_ROOT = androidHome;
          ANDROID_BUILD_TOOLS = "${androidHome}/build-tools/36.0.0";
          JAVA_HOME = "${jdk.home}";

          shellHook = ''
            echo "mpvium: JDK $(java -version 2>&1 | head -n1)"
            echo "mpvium: Android SDK $ANDROID_HOME"
            echo "Build:  ./gradlew assembleStandardDebug"
            echo "Or:     nix run .#debug"
          '';
        };

      # `nix run .#<name>` entry points. They reuse the same SDK/JDK closure as
      # the dev shell but are not hermetic: Gradle still resolves dependencies
      # from the network, exactly as it does inside `nix develop`.
      mkApp =
        pkgs: name: gradleArgs:
        let
          sdk = androidSdkFor pkgs;
          jdk = pkgs.jdk17;
        in
        {
          type = "app";
          program = "${pkgs.writeShellApplication {
            inherit name;
            runtimeInputs = [
              sdk
              jdk
              pkgs.git
              pkgs.which
            ];
            text = ''
              export ANDROID_HOME="${sdk}/libexec/android-sdk"
              export ANDROID_SDK_ROOT="$ANDROID_HOME"
              export JAVA_HOME="${jdk.home}"

              cd "$(git rev-parse --show-toplevel)" || exit 1
              exec ./gradlew ${gradleArgs} "$@"
            '';
          }}/bin/${name}";
          meta.description = "Run ./gradlew ${gradleArgs} with the pinned toolchain";
        };

      ciGradleArgs = "assembleStandardRelease bundleStandardRelease --parallel -x lintVitalStandardRelease -PciDebugSigning -PbundleBuild -PciFastBuild";
    in
    {
      formatter = nixpkgs.lib.genAttrs systems (
        system: (mkPkgs system).nixfmt-rfc-style
      );

      packages = nixpkgs.lib.genAttrs systems (
        system:
        let
          pkgs = mkPkgs system;
        in
        {
          android-sdk = androidSdkFor pkgs;
          default = androidSdkFor pkgs;
        }
      );

      devShells = nixpkgs.lib.genAttrs systems (
        system: {
          default = mkDevShell (mkPkgs system);
        }
      );

      apps = nixpkgs.lib.genAttrs systems (
        system:
        let
          pkgs = mkPkgs system;
        in
        {
          default = mkApp pkgs "build" "assembleStandardRelease";
          build = mkApp pkgs "build" "assembleStandardRelease";
          debug = mkApp pkgs "debug" "assembleStandardDebug";
          preview = mkApp pkgs "preview" "assembleStandardPreview";
          test = mkApp pkgs "test" "testStandardDebugUnitTest";
          bundle = mkApp pkgs "bundle" "bundleStandardRelease -PbundleBuild";
          ci = mkApp pkgs "ci" ciGradleArgs;
        }
      );

      checks = nixpkgs.lib.genAttrs systems (
        system:
        let
          pkgs = mkPkgs system;
        in
        {
          formatting = pkgs.runCommand "mpvium-nix-formatting" {
            nativeBuildInputs = [ pkgs.nixfmt-rfc-style ];
          } ''
            cd ${self}
            find . -name '*.nix' -print0 | xargs -0 nixfmt --check
            touch $out
          '';
        }
      );
    };
}
