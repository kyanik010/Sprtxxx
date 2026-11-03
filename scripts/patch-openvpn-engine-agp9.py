from pathlib import Path

ENGINE_BUILD = Path(
    "vendor/OpenVPNGateClientApp/src/external/OpenVPNEngine/main/build.gradle.kts"
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
    '            assets.directories.addAll(listOf(file("src/main/assets"), file("build/ovpnassets")))',
    "deprecated assets.srcDirs declaration",
)

text = replace_once(
    text,
    '''            java.srcDirs(
                "src/main/java",
                "src/skeleton/java"
            )''',
    '''            java.directories.addAll(
                listOf(file("src/main/java"), file("src/skeleton/java"))
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
