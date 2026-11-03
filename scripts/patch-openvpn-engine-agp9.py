from pathlib import Path

ENGINE_BUILD = Path(
    "vendor/OpenVPNGateClientApp/src/external/OpenVPNEngine/main/build.gradle.kts"
)

CORE_BUILD = Path(
    "vendor/OpenVPNGateClientApp/src/core/build.gradle.kts"
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
    '''            java.srcDirs(
                "src/main/java",
                "src/skeleton/java"
            )''',
    '''            java.directories.addAll(
                listOf("src/main/java", "src/skeleton/java")
            )''',
    "deprecated java.srcDirs declaration",
)

text = replace_once(
    text,
    '''    kotlinOptions {
        jvmTarget = "17"
    }''',
    '''    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }''',
    "deprecated Kotlin compiler options block",
)

ENGINE_BUILD.write_text(text, encoding="utf-8")
print(f"Applied AGP 9 public DSL compatibility patch to {ENGINE_BUILD}")

if not CORE_BUILD.is_file():
    raise SystemExit(f"OpenVPN core build file not found: {CORE_BUILD}")

core_text = CORE_BUILD.read_text(encoding="utf-8")
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
