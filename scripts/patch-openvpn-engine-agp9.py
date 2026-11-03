from pathlib import Path

ENGINE_BUILD = Path(
    "vendor/OpenVPNGateClientApp/src/external/OpenVPNEngine/main/build.gradle.kts"
)

CORE_BUILD = Path(
    "vendor/OpenVPNGateClientApp/src/core/build.gradle.kts"
)

TEST_ROOT = Path("vendor/OpenVPNGateClientApp/src/core/src/test/java/com/yahorzabotsin/openvpnclientgate/core")

TEST_CONFIGS = (
    (
        TEST_ROOT / "ui/CountryListAdapterTest.kt",
        '    packageName = "com.yahorzabotsin.openvpnclientgate.core"\n',
    ),
    (
        TEST_ROOT / "ui/ServerPickerAdapterCityUtcTest.kt",
        '    packageName = "com.yahorzabotsin.openvpnclientgate.core"\n',
    ),
    (
        TEST_ROOT / "ui/ServerPickerAdapterTest.kt",
        '    packageName = "com.yahorzabotsin.openvpnclientgate.core"\n',
    ),
    (
        TEST_ROOT / "ui/common/components/ConnectionControlsPresenterTest.kt",
        ', packageName = "com.yahorzabotsin.openvpnclientgate.core"',
    ),
    (
        TEST_ROOT / "ui/common/components/SpeedometerViewTest.kt",
        '    packageName = "com.yahorzabotsin.openvpnclientgate.core"\n',
    ),
)


def replace_once(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count != 1:
        raise SystemExit(
            f"OpenVPN engine patch stopped: expected exactly one {label}, found {count}"
        )
    return source.replace(old, new, 1)


if not ENGINE_BUILD.is_file():
    raise SystemExit(f"OpenVPN engine build file not found: {ENGINE_BUILD}")

text = ENGINE_BUILD.read_text(encoding="utf-8")

text = replace_once(
    text,
    "android {",
    'extensions.configure<com.android.build.api.dsl.LibraryExtension>("android") {',
    "legacy Android library DSL block",
)

text = replace_once(
    text,
    "        targetSdk = 37\n",
    "",
    "library targetSdk declaration",
)

text = replace_once(
    text,
    '            assets.srcDirs("src/main/assets", "build/ovpnassets")',
    '            assets.directories.addAll(listOf("src/main/assets", "build/ovpnassets"))',
    "deprecated assets.srcDirs declaration",
)

text = replace_once(
    text,
    """            java.srcDirs(
                "src/main/java",
                "src/skeleton/java"
            )""",
    """            java.directories.addAll(
                listOf("src/main/java", "src/skeleton/java")
            )""",
    "deprecated java.srcDirs declaration",
)

text = replace_once(
    text,
    """    kotlinOptions {
        jvmTarget = "17"
    }""",
    """    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }""",
    "deprecated Kotlin compiler options block",
)

ENGINE_BUILD.write_text(text, encoding="utf-8")
print(f"Applied AGP 9 public DSL compatibility patch to {ENGINE_BUILD}")

if not CORE_BUILD.is_file():
    raise SystemExit(f"OpenVPN core build file not found: {CORE_BUILD}")

core_text = CORE_BUILD.read_text(encoding="utf-8")
core_text = replace_once(
    core_text,
    "    compileSdk = 36\n",
    "    compileSdk = 37\n",
    "vpncore compileSdk required by openVpnEngine",
)
core_text = replace_once(
    core_text,
    """    buildFeatures {
        viewBinding = true
        buildConfig = true
    }""",
    """    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
    }""",
    "AGP 9 resource-values build feature",
)
CORE_BUILD.write_text(core_text, encoding="utf-8")
print(f"Enabled AGP 9 resource values for {CORE_BUILD}")

for test_file, obsolete_config in TEST_CONFIGS:
    if not test_file.is_file():
        raise SystemExit(f"OpenVPN test compatibility patch stopped: missing {test_file}")
    test_text = test_file.read_text(encoding="utf-8")
    test_text = replace_once(
        test_text,
        obsolete_config,
        "",
        f"obsolete Robolectric packageName setting in {test_file}",
    )
    test_text = replace_once(
        test_text,
        'manifest = "src/main/AndroidManifest.xml"',
        'manifest = "vendor/OpenVPNGateClientApp/src/core/src/test/AndroidManifest.xml"',
        f"Robolectric test manifest path in {test_file}",
    )
    test_file.write_text(test_text, encoding="utf-8")
    print(f"Updated Robolectric manifest configuration in {test_file}")

# Robolectric 4.10.2 no longer accepts @Config(packageName=...). The production
# library manifest intentionally relies on the Gradle namespace and has no
# package attribute, so use a minimal test-only manifest that supplies it.
TEST_MANIFEST = Path("vendor/OpenVPNGateClientApp/src/core/src/test/AndroidManifest.xml")
TEST_MANIFEST.parent.mkdir(parents=True, exist_ok=True)
TEST_MANIFEST.write_text(
    '<?xml version="1.0" encoding="utf-8"?>\\n'
    '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
    'package="com.yahorzabotsin.openvpnclientgate.core">\\n'
    '</manifest>\\n',
    encoding="utf-8",
)
print(f"Created Robolectric test manifest at {TEST_MANIFEST}")
