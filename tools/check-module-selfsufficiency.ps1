# check-module-selfsufficiency.ps1 -- P7b module-jar self-sufficiency gate (static, read-only).
#
# WHY THIS EXISTS
#   After P7a the three content modules are meant to be real, individually installable mod
#   jars (coe / cews / transmutation): each carries its own neoforge.mods.toml, its own
#   @JeiPlugin, its own copy of the core library and its own assets.  Nothing static
#   guaranteed that.  P7a shipped a coe.jar containing 7 mixin classes and ZERO mixin
#   configs, so on a "install coe.jar only" setup all 7 were silently inert -- including
#   two accessor mixins whose interfaces are plain interfaces in the jar, which turns into
#   a ClassCastException (not a graceful degradation) the moment that render path runs.
#   This script is that missing gate: it asserts, from the built jars alone, that a module
#   jar carries the machinery its own code needs.
#
# WHAT IT CHECKS (every assertion prints PASS/FAIL together with the actual numbers)
#   A. the expected jars exist (otherwise the message says which Gradle task to run).
#   B. per module jar (coe / cews / transmutation):
#      B1 [[mods]] is non-empty and the declared modId equals the expected one.
#      B2 mixin config self-consistency:
#           N = number of [[mixins]] blocks in the EXPANDED META-INF/neoforge.mods.toml
#           M = number of *.mixins.json entries at the jar ROOT
#           require N == M; if the jar contains any class under
#           com/hjmmd_8/createoreexpansion/mixin/ then require N >= 1 (this is exactly the
#           P7a hole: classes present, declaration absent); if it contains none, require
#           N == 0 (never declare a config that does not exist);
#           every  config = "<name>"  must resolve to a root-level entry of the SAME jar;
#           every class the config names (mixins[] / client[] / plugin) must exist in the
#           SAME jar -- i.e. "the config follows the classes".
#           Mechanism: FML registers configs per mod file
#           (LoadingModList.addMixinConfigs() -> file.getMixinConfigs() -> the file's OWN
#           [[mixins]] table), while Mixin resolves the config NAME as a classpath resource
#           (MixinConfig.create -> MixinServiceModLauncher.getResourceAsStream ->
#           contextClassLoader).  So a per-module config works, but only when the
#           [[mixins]] block and the resource live in the same jar.
#      B3 at least one @JeiPlugin class under com/hjmmd_8/createoreexpansion/compat/jei/,
#         confirmed with javap -v (the annotation is RuntimeInvisible, so "the source says
#         @JeiPlugin" is not the same evidence as "the built class carries it").
#      B4 assets/createoreexpansion/lang/en_us.json and zh_cn.json exist inside the jar,
#         parse as JSON, and their key set AND values are identical to the root's generated
#         lang file.  (P7b: each module also carries a full copy of the root language file,
#         247-odd bare-string keys included, so a module jar installed on its own never
#         shows raw translation keys.  Key/value equality is asserted so the copy can never
#         drift into "a second, edited language file".)
#      B5 a META-INF/jarjar/ entry provides the shared core library (*coe_core*), i.e. the
#         module does not depend on some other jar for its own library.
#      B6 @EventBusSubscriber audit (one javap -v pass over every class of the jar): a class
#         whose annotation carries modid="X" must have X == the jar's own [[mods]] id.
#         The rule is "the class's modid == the id of the mod file it lives in"
#         (AutomaticEventSubscriber.inject filters by mod.getModId()); a mismatch is
#         completely silent -- no warning, no error, the subscriber just never fires.
#         A class that omits modid is fine: FML falls back to the file's own id.
#      B7 (P7c) the SHARED data pack set -- core/src/main/resources/data/** -- is present in
#         this jar and byte-identical (SHA-256) to the core source.  Those are the hand-written
#         data files whose referenced ids are all vanilla/external or belong to two or more
#         layers, so no single module may own them; without them a single-module install
#         silently loses the enchantment tags, the loot-modifier injections and the curios
#         player slots.  A shared set of 0 files fails: the shared home must exist.
#   C. the core jar ([[mods]] empty by design: FMLModType GAMELIBRARY, no ModContainer) must
#      contain NO class carrying @EventBusSubscriber.  Such a subscriber can never be
#      injected in production -- P7a's AllConfig defect, where the entire common config
#      silently stopped being loaded while dev looked perfectly healthy.
#   D. shared-wiring source assertions over the coe / cews / transmutation sources:
#      D1 each module references LayerBootstrap.ensureAttached( at least once.
#      D2 no module source references LayerRecipeType.registerOn, LayerCreativeTab.registerOn
#         or MachineRotatePayload.  Those are the exactly-once, cross-layer shared
#         registrations (and the shared network payload); their only call site is core's
#         LayerBootstrap#ensureAttached, because DeferredRegister#register() throws on a
#         second bus and NetworkRegistry throws on a duplicate payload id.
#      D3 every payload type a module registers with registerPayloads belongs to that module
#         (its .java file lives under that module's source root).  Modules legitimately
#         register their OWN payloads (coe: SkillSettingsPayload, cews:
#         EnergyFieldSyncPayload) -- that is NOT a violation, so the rule is ownership, not
#         "never call registerPayloads".
#      D4 no module source calls a core Layer* register's registration entry point directly
#         (LayerXxx.register( / LayerXxx.registerOn().  LayerCreativeTab.registerAll( is
#         deliberately NOT forbidden: that is the sanctioned per-layer creative-tab API, where
#         each layer registers its own tabs into an already-attached register.
#      D5 (informational, never fails) the module-owned "NAME.register(modEventBus)" sites,
#         listed with file + register constant, so the number stays visible: the literal rule
#         "modules must not call DeferredRegister.register(" would be red by design, because
#         those calls are module-owned registries that have no business going through core
#         (core may not know any layer).
#      Line comments are stripped before matching D1-D4, so javadoc that merely mentions one
#      of these names does not count as a call site.  The strip is deliberately naive (it
#      also cuts at "//" inside a string literal); that can only hide a violation sitting
#      after such a literal on the same line, which none of these patterns can be.
#   E. hand-written data/** home assertions (P7c):
#      E1 every file under <module>/src/main/resources/data/** is packaged in that module's own
#         jar, byte-identical.  This is what makes the per-layer half of the P7c move real.
#      E2 src/main/resources/data/** holds no file at all.  The root project (coe_integration)
#         is the integration layer and is NOT shipped, so a data file left there reaches no
#         module jar; every one of them belongs in core (shared) or in its owning module.
#         This is the assertion that was red before P7c (27 stranded files) and is the durable
#         form of that hole -- it cannot come back by someone re-adding a file to the root tree.
#
# KNOWN GAP (do not mistake this script for proof of runtime behaviour)
#   Everything here is static jar/source inspection.  It cannot prove that Mixin really
#   applied a config, that JEI really assembled a category, or that the game boots with
#   only one module jar installed.  Those need runData (config registration) and a real
#   game launch with a single jar in mods/.
#
# USAGE
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-module-selfsufficiency.ps1
#   Exit code: 0 = every assertion holds, 1 = at least one failed.
#   Prerequisite: .\gradlew.bat processResources jar --rerun-tasks
#
# NOTE: this file is deliberately pure ASCII (Windows PowerShell 5.1 reads a BOM-less .ps1
# as ANSI, which mangles non-ASCII text and can swallow quotes mid-script).  Read-only: it
# writes nothing into the repository, only into %TEMP%.

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

# ---------------------------------------------------------------------------
# JDK tools.  jar/javap come from JAVA_HOME when it is set, else from PATH.
# ---------------------------------------------------------------------------
function Resolve-JdkTool {
    param([string]$Name)
    if ($env:JAVA_HOME) {
        $candidate = Join-Path (Join-Path $env:JAVA_HOME 'bin') ($Name + '.exe')
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    $found = Get-Command $Name -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }
    throw "Cannot find '$Name'. Set JAVA_HOME to a JDK 21 installation."
}

$jarExe = Resolve-JdkTool 'jar'
$javapExe = Resolve-JdkTool 'javap'

# ---------------------------------------------------------------------------
# EXPECTED JARS -- the single table that knows about modules.  Add a module here
# the same day its Gradle project starts producing a jar that is shipped alone;
# a module missing from this table is simply not audited (exit stays 0).
# ---------------------------------------------------------------------------
$modules = [ordered]@{
    'coe' = @{
        jar    = 'coe\build\libs\createoreexpansion-1.0.0.jar'
        modId  = 'createoreexpansion'
        src    = 'coe\src\main\java'
    }
    'cews' = @{
        jar    = 'cews\build\libs\cews-1.0.0.jar'
        modId  = 'cews'
        src    = 'cews\src\main\java'
    }
    'transmutation' = @{
        jar    = 'transmutation\build\libs\transmutation-1.0.0.jar'
        modId  = 'transmutation'
        src    = 'transmutation\src\main\java'
    }
}

$coreJar = 'core\build\libs\coe_core-1.0.0.jar'

$langLocales = @('en_us', 'zh_cn')
$langRoot    = 'src\generated\resources\assets\createoreexpansion\lang'
$langInJar   = 'assets/createoreexpansion/lang'

# Names that must never be touched from a module source (see D2 in the header).
$sharedWiringNames = @(
    'LayerRecipeType.registerOn',
    'LayerCreativeTab.registerOn',
    'MachineRotatePayload'
)

$tempRoot = Join-Path $env:TEMP ('coe-selfsuf-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null

$script:checkCount = 0
$script:failures = @()

function Write-Check {
    param([bool]$Ok, [string]$Label, [string]$Detail)
    $script:checkCount = $script:checkCount + 1
    $suffix = ''
    if ($Detail) { $suffix = ' -- ' + $Detail }
    if ($Ok) {
        Write-Host ('  PASS  ' + $Label + $suffix)
    } else {
        Write-Host ('  FAIL  ' + $Label + $suffix)
        $script:failures = $script:failures + $Label
    }
}

function Get-JarEntries {
    param([string]$JarPath)
    return @(& $jarExe tf $JarPath)
}

function Get-JarEntryText {
    param([string]$JarPath, [string]$Entry)
    $dir = Join-Path $tempRoot ([guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Push-Location $dir
    try { [void](& $jarExe xf $JarPath $Entry 2>&1) } finally { Pop-Location }
    $file = Join-Path $dir ($Entry -replace '/', '\')
    if (-not (Test-Path -LiteralPath $file)) { return $null }
    return (Get-Content -LiteralPath $file -Raw -Encoding UTF8)
}

# SHA-256 of one jar entry's BYTES (not its decoded text: B7/E1 compare data files byte for
# byte, and a CR/LF drift is exactly the kind of silent difference worth catching).
function Get-JarEntryHash {
    param([string]$JarPath, [string]$Entry)
    $dir = Join-Path $tempRoot ([guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Push-Location $dir
    try { [void](& $jarExe xf $JarPath $Entry 2>&1) } finally { Pop-Location }
    $file = Join-Path $dir ($Entry -replace '/', '\')
    if (-not (Test-Path -LiteralPath $file)) { return $null }
    return (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash
}

# <repo>\<root>\...\data\<rest>.json  ->  the jar entry name "data/<rest>.json"
function Get-DataJarEntry {
    param([string]$FullName)
    $i = $FullName.IndexOf('\data\')
    if ($i -lt 0) { return $null }
    return (($FullName.Substring($i + 1)) -replace '\\', '/')
}

# One javap -v pass per jar; returns a hashtable classfile-path -> record.
# Records: Ebs (bool), Modid (string or $null), Jei (bool).
function Get-CompiledAnnotations {
    param([string]$JarPath, [string[]]$ClassNames)
    $records = @{}
    if ($ClassNames.Count -eq 0) { return $records }

    $outFile = Join-Path $tempRoot ('javap-' + [guid]::NewGuid().ToString('N') + '.txt')
    & $javapExe -v -p -cp $JarPath @ClassNames 2>&1 |
        Out-File -LiteralPath $outFile -Encoding UTF8

    $current = $null
    $hasEbs = $false
    $modid = $null
    $hasJei = $false

    foreach ($line in (Get-Content -LiteralPath $outFile -Encoding UTF8)) {
        if ($line.StartsWith('Classfile ')) {
            if ($current) {
                $records[$current] = [pscustomobject]@{ Ebs = $hasEbs; Modid = $modid; Jei = $hasJei }
            }
            $current = ($line.Substring(10)).Trim()
            $hasEbs = $false
            $modid = $null
            $hasJei = $false
            continue
        }
        if ($line -like '*Lnet/neoforged/fml/common/EventBusSubscriber;*') { $hasEbs = $true }
        elseif ($line -like '*Lmezz/jei/api/JeiPlugin;*') { $hasJei = $true }
        elseif ($line -match 'modid="([^"]*)"') { if (-not $modid) { $modid = $Matches[1] } }
    }
    if ($current) {
        $records[$current] = [pscustomobject]@{ Ebs = $hasEbs; Modid = $modid; Jei = $hasJei }
    }
    return $records
}

# Extract the [[mods]] modId values from an expanded neoforge.mods.toml (top-level only:
# a [[dependencies."..."]] block also has modId = "...", and those must not count).
function Get-DeclaredModIds {
    param([string]$TomlText)
    $ids = @()
    $inside = $false
    foreach ($line in ($TomlText -split "`n")) {
        # A table header may carry a trailing comment: the root template ships "[[mods]] #mandatory".
        if ($line -match '^\s*\[\[\s*([A-Za-z0-9_.-]+)\s*\]\](?:\s*#.*)?$') {
            $inside = ($Matches[1] -eq 'mods')
            continue
        }
        if ($line -match '^\s*\[') { $inside = $false; continue }
        if ($inside -and $line -match '^\s*modId\s*=\s*"([^"]*)"') { $ids = $ids + $Matches[1] }
    }
    return @($ids)
}

function Get-TomlTableCount {
    param([string]$TomlText, [string]$Table)
    $pattern = '^\s*\[\[\s*' + [regex]::Escape($Table) + '\s*\]\](?:\s*#.*)?$'
    return @(($TomlText -split "`n") | Where-Object { $_ -match $pattern }).Count
}

function Get-TomlStringValues {
    param([string]$TomlText, [string]$Key)
    $pattern = '^\s*' + [regex]::Escape($Key) + '\s*=\s*"([^"]*)"'
    return @(($TomlText -split "`n") | ForEach-Object {
        if ($_ -match $pattern) { $Matches[1] }
    })
}

# Java source with line and block comments removed (see the header for the caveat).
function Get-SourceCode {
    param([string]$Path)
    $raw = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
    $raw = [regex]::Replace($raw, '(?s)/\*.*?\*/', '')
    $raw = [regex]::Replace($raw, '(?m)//.*$', '')
    return $raw
}

function Get-LangMap {
    param([string]$Text)
    $obj = $Text | ConvertFrom-Json
    $map = @{}
    foreach ($prop in $obj.PSObject.Properties) { $map[$prop.Name] = [string]$prop.Value }
    return $map
}

Write-Host 'module self-sufficiency audit (P7b): jars + sources, static, read-only'
Write-Host ''

# ---------------------------------------------------------------------------
# A. jars exist
# ---------------------------------------------------------------------------
Write-Host '[A] expected jars'
$missing = @()
foreach ($name in $modules.Keys) {
    $path = Join-Path $repoRoot $modules[$name].jar
    if (Test-Path -LiteralPath $path) {
        $item = Get-Item -LiteralPath $path
        Write-Host ("      {0,-14} {1,10} B  {2}" -f $name, $item.Length, $item.LastWriteTime)
    } else {
        Write-Host ("      {0,-14} MISSING: {1}" -f $name, $modules[$name].jar)
        $missing = $missing + $name
    }
}
$corePath = Join-Path $repoRoot $coreJar
if (Test-Path -LiteralPath $corePath) {
    $coreItem = Get-Item -LiteralPath $corePath
    Write-Host ("      {0,-14} {1,10} B  {2}" -f 'core', $coreItem.Length, $coreItem.LastWriteTime)
} else {
    Write-Host ("      {0,-14} MISSING: {1}" -f 'core', $coreJar)
    $missing = $missing + 'core'
}
Write-Check ($missing.Count -eq 0) 'jars present' `
    $(if ($missing.Count -eq 0) { "$($modules.Count + 1) jars" } else { 'missing: ' + ($missing -join ', ') + ' -- run .\gradlew.bat processResources jar --rerun-tasks' })
Write-Host ''

if ($missing.Count -gt 0) {
    Write-Host 'ABORT: cannot audit jars that do not exist.'
    exit 1
}

# ---------------------------------------------------------------------------
# B. per module jar
# ---------------------------------------------------------------------------
foreach ($name in $modules.Keys) {
    $jarPath = Join-Path $repoRoot $modules[$name].jar
    $expectId = $modules[$name].modId
    Write-Host ("[B] module '" + $name + "'  (" + $modules[$name].jar + ")")

    $entries = @(Get-JarEntries $jarPath)

    $toml = Get-JarEntryText $jarPath 'META-INF/neoforge.mods.toml'
    if (-not $toml) {
        Write-Check $false ($name + ' META-INF/neoforge.mods.toml readable') 'entry missing from the jar'
        Write-Host ''
        continue
    }

    # B1 -- [[mods]] modId
    $modIds = @(Get-DeclaredModIds $toml)
    Write-Check (($modIds.Count -eq 1) -and ($modIds[0] -eq $expectId)) `
        ($name + ' B1 [[mods]] declares its own modId') `
        ("ids=[" + ($modIds -join ',') + "] expected=" + $expectId)

    # B2 -- mixin config self-consistency
    $mixinsBlocks = Get-TomlTableCount $toml 'mixins'
    $configNames = @(Get-TomlStringValues $toml 'config')
    $rootMixinsJson = @($entries | Where-Object { $_ -match '^[^/]+\.mixins\.json$' })
    $mixinClasses = @($entries | Where-Object { $_ -match '^com/hjmmd_8/createoreexpansion/mixin/.+\.class$' })

    $nEqualsM = ($mixinsBlocks -eq $rootMixinsJson.Count)
    Write-Check $nEqualsM ($name + ' B2a [[mixins]] count == *.mixins.json count') `
        ("N=" + $mixinsBlocks + " M=" + $rootMixinsJson.Count + " configs=[" + ($configNames -join ',') + "] files=[" + ($rootMixinsJson -join ',') + "]")

    $pairingOk = $true
    if ($mixinClasses.Count -gt 0 -and $mixinsBlocks -lt 1) { $pairingOk = $false }
    if ($mixinClasses.Count -eq 0 -and $mixinsBlocks -ne 0) { $pairingOk = $false }
    Write-Check $pairingOk ($name + ' B2b mixin classes and [[mixins]] declaration agree') `
        ("mixinClasses=" + $mixinClasses.Count + " mixinsBlocks=" + $mixinsBlocks + " (classes>0 requires blocks>=1; classes=0 requires blocks=0)")

    $configResolves = $true
    foreach ($cfgName in $configNames) {
        if (-not ($entries -contains $cfgName)) { $configResolves = $false }
    }
    Write-Check $configResolves ($name + ' B2c every config name is a root-level entry of the same jar') `
        ("configs=[" + ($configNames -join ',') + "]")

    $classResolves = $true
    $classProblems = @()
    foreach ($cfgName in $configNames) {
        $cfgText = Get-JarEntryText $jarPath $cfgName
        if (-not $cfgText) { $classResolves = $false; $classProblems = $classProblems + ($cfgName + ': unreadable'); continue }
        $cfg = $cfgText | ConvertFrom-Json
        $pkgPath = ([string]$cfg.package) -replace '\.', '/'
        $named = @()
        foreach ($m in @($cfg.mixins)) { if ($m) { $named = $named + [string]$m } }
        foreach ($m in @($cfg.client)) { if ($m) { $named = $named + [string]$m } }
        foreach ($m in $named) {
            $entry = $pkgPath + '/' + ($m -replace '\.', '/') + '.class'
            if (-not ($entries -contains $entry)) { $classResolves = $false; $classProblems = $classProblems + $entry }
        }
        if ($cfg.plugin) {
            $entry = ([string]$cfg.plugin -replace '\.', '/') + '.class'
            if (-not ($entries -contains $entry)) { $classResolves = $false; $classProblems = $classProblems + $entry }
        }
    }
    Write-Check $classResolves ($name + ' B2d every class named by the mixin config exists in the same jar') `
        $(if ($classResolves) { 'all named classes present' } else { 'missing: ' + ($classProblems -join ', ') })

    # B3 / B6 -- one javap -v pass over every class of the jar
    $classEntries = @($entries | Where-Object { $_ -like 'com/hjmmd_8/*.class' })
    $classNames = @($classEntries | ForEach-Object { ($_ -replace '\.class$', '') -replace '/', '.' })
    $records = Get-CompiledAnnotations $jarPath $classNames

    $jeiClasses = @()
    foreach ($cls in $records.Keys) {
        if ($records[$cls].Jei -and ($cls -like '*compat/jei/*') -and ($cls -like '*!/com/hjmmd_8/createoreexpansion/*')) {
            $jeiClasses = $jeiClasses + $cls
        }
    }
    Write-Check ($jeiClasses.Count -ge 1) ($name + ' B3 >=1 @JeiPlugin under compat/jei/ (javap -v)') `
        ("count=" + $jeiClasses.Count + " -> " + (($jeiClasses | ForEach-Object { ($_ -split '!/')[-1] }) -join ', '))

    # B4 -- language self-sufficiency
    foreach ($locale in $langLocales) {
        $entry = $langInJar + '/' + $locale + '.json'
        $inJar = $entries -contains $entry
        $rootFile = Join-Path $repoRoot ($langRoot + '\' + $locale + '.json')
        $rootExists = Test-Path -LiteralPath $rootFile
        if (-not $inJar) {
            Write-Check $false ($name + ' B4 lang/' + $locale + '.json inside the jar') 'entry missing'
            continue
        }
        if (-not $rootExists) {
            Write-Check $false ($name + ' B4 lang/' + $locale + '.json matches the root file') ('root file missing: ' + $langRoot + '\' + $locale + '.json')
            continue
        }
        $jarText = Get-JarEntryText $jarPath $entry
        $rootText = Get-Content -LiteralPath $rootFile -Raw -Encoding UTF8
        $jarMap = Get-LangMap $jarText
        $rootMap = Get-LangMap $rootText
        $jarKeys = @($jarMap.Keys | Sort-Object)
        $rootKeys = @($rootMap.Keys | Sort-Object)
        $keyDiff = @(Compare-Object -ReferenceObject $rootKeys -DifferenceObject $jarKeys -CaseSensitive)
        $valueDiff = 0
        if ($keyDiff.Count -eq 0) {
            foreach ($k in $jarKeys) { if ($rootMap[$k] -cne $jarMap[$k]) { $valueDiff = $valueDiff + 1 } }
        }
        $langOk = ($keyDiff.Count -eq 0) -and ($valueDiff -eq 0)
        Write-Check $langOk ($name + ' B4 lang/' + $locale + '.json is a full, unedited copy of the root file') `
            ("jarKeys=" + $jarKeys.Count + " rootKeys=" + $rootKeys.Count + " keyDiff=" + $keyDiff.Count + " valueDiff=" + $valueDiff)
    }

    # B5 -- nested shared core library
    $jarJarCore = @($entries | Where-Object { $_ -match '^META-INF/jarjar/.+coe_core.*\.jar$' })
    Write-Check ($jarJarCore.Count -ge 1) ($name + ' B5 META-INF/jarjar/ provides the shared core library') `
        ("entries=" + $jarJarCore.Count + " [" + ($jarJarCore -join ',') + "]")

    # B6 -- @EventBusSubscriber modid == this jar's mod id
    $ebsClasses = @()
    $ebsMismatch = @()
    $ebsNoModid = 0
    foreach ($cls in $records.Keys) {
        $rec = $records[$cls]
        if (-not $rec.Ebs) { continue }
        $short = ($cls -split '!/')[-1]
        if (-not $rec.Modid) { $ebsNoModid = $ebsNoModid + 1; continue }
        $ebsClasses = $ebsClasses + ($short + ' modid=' + $rec.Modid)
        if ($rec.Modid -ne $expectId) { $ebsMismatch = $ebsMismatch + ($short + ' modid=' + $rec.Modid) }
    }
    Write-Check ($ebsMismatch.Count -eq 0) ($name + ' B6 every @EventBusSubscriber modid == this jar modId') `
        ("withModid=" + $ebsClasses.Count + " withoutModid=" + $ebsNoModid + " mismatches=" + $ebsMismatch.Count + $(if ($ebsMismatch.Count -gt 0) { ' [' + ($ebsMismatch -join '; ') + ']' } else { '' }))

    # B7 -- shared data pack set, carried byte for byte (P7c).
    # core/src/main/resources/data/** is the home of the hand-written data files whose ids are
    # either all vanilla/external or spread over two or more layers (P7c task 1 verdict), so no
    # single module may own them: every module must physically carry a copy.  A module jar
    # without them still builds and still boots -- the enchantment tags, the loot modifier
    # injections and the curios player slots just silently do not exist for that install.
    # The copy is compared byte for byte (SHA-256) so it can never drift into "a second,
    # edited data file", the same way B4 pins the language copy.
    $sharedDataRootRel = 'core\src\main\resources\data'
    $sharedDataRoot = Join-Path $repoRoot $sharedDataRootRel
    $sharedFiles = @()
    if (Test-Path -LiteralPath $sharedDataRoot) {
        $sharedFiles = @(Get-ChildItem -LiteralPath $sharedDataRoot -Recurse -File -Filter '*.json')
    }
    $sharedMissing = @()
    $sharedMismatch = @()
    foreach ($sf in $sharedFiles) {
        $entry = Get-DataJarEntry $sf.FullName
        if (-not $entry) { continue }
        if (-not ($entries -contains $entry)) { $sharedMissing = $sharedMissing + $entry; continue }
        if ((Get-JarEntryHash $jarPath $entry) -ne (Get-FileHash -LiteralPath $sf.FullName -Algorithm SHA256).Hash) {
            $sharedMismatch = $sharedMismatch + $entry
        }
    }
    $sharedOk = ($sharedFiles.Count -gt 0) -and ($sharedMissing.Count -eq 0) -and ($sharedMismatch.Count -eq 0)
    Write-Check $sharedOk ($name + ' B7 carries every shared data/** file byte-identical') `
        ("sharedHome=" + $sharedDataRootRel + " sharedFiles=" + $sharedFiles.Count + " missing=" + $sharedMissing.Count + " hashMismatch=" + $sharedMismatch.Count + `
         $(if ($sharedMissing.Count -gt 0) { ' missing=[' + ($sharedMissing -join ', ') + ']' } else { '' }) + `
         $(if ($sharedMissing.Count -eq 0 -and $sharedMismatch.Count -eq 0 -and $sharedFiles.Count -gt 0) { ' [' + (($sharedFiles | ForEach-Object { Get-DataJarEntry $_.FullName }) -join ', ') + ']' } else { '' }))

    Write-Host ''
}

# ---------------------------------------------------------------------------
# C. the library jar must not carry @EventBusSubscriber
# ---------------------------------------------------------------------------
Write-Host ("[C] core library jar (" + $coreJar + ")")
$coreEntries = @(Get-JarEntries $corePath)
$coreToml = Get-JarEntryText $corePath 'META-INF/neoforge.mods.toml'
$coreModIds = @()
if ($coreToml) { $coreModIds = @(Get-DeclaredModIds $coreToml) }
Write-Check ($coreModIds.Count -eq 0) 'core C1 [[mods]] is empty (it is a library, not a mod)' `
    ("tomlPresent=" + [bool]$coreToml + " ids=[" + ($coreModIds -join ',') + "]")

$coreClassEntries = @($coreEntries | Where-Object { $_ -like 'com/hjmmd_8/*.class' })
$coreClassNames = @($coreClassEntries | ForEach-Object { ($_ -replace '\.class$', '') -replace '/', '.' })
$coreRecords = Get-CompiledAnnotations $corePath $coreClassNames
$coreSubscribers = @()
foreach ($cls in $coreRecords.Keys) {
    if ($coreRecords[$cls].Ebs) { $coreSubscribers = $coreSubscribers + (($cls -split '!/')[-1]) }
}
Write-Check ($coreSubscribers.Count -eq 0) 'core C2 no @EventBusSubscriber (it has no ModContainer)' `
    ("classesScanned=" + $coreClassNames.Count + " withAnnotation=" + $coreSubscribers.Count + $(if ($coreSubscribers.Count -gt 0) { ' [' + ($coreSubscribers -join ', ') + ']' } else { '' }))
Write-Host ''

# ---------------------------------------------------------------------------
# D. shared-wiring source assertions
# ---------------------------------------------------------------------------
Write-Host '[D] shared-wiring source assertions (module sources, comments stripped)'
foreach ($name in $modules.Keys) {
    $srcPath = Join-Path $repoRoot $modules[$name].src
    if (-not (Test-Path -LiteralPath $srcPath)) {
        Write-Check $false ($name + ' D source root exists') $modules[$name].src
        continue
    }
    $files = @(Get-ChildItem -LiteralPath $srcPath -Recurse -File -Filter '*.java')

    $ensureHits = @()
    $forbiddenHits = @{}
    foreach ($f in $sharedWiringNames) { $forbiddenHits[$f] = @() }
    $payloadTypes = @{}
    $layerRegisterSites = @()
    $deferredRegisterSites = @()

    foreach ($f in $files) {
        $code = Get-SourceCode $f.FullName
        $rel = $f.FullName.Substring($srcPath.Length).TrimStart('\', '/')
        if ($code -match 'LayerBootstrap\.ensureAttached\s*\(') { $ensureHits = $ensureHits + $rel }
        foreach ($needle in $sharedWiringNames) {
            if ($code -match [regex]::Escape($needle)) { $forbiddenHits[$needle] = $forbiddenHits[$needle] + $rel }
        }
        # Qualified payload registration sites, both spellings:
        #   "SomePayload::registerPayloads"   (method-reference listener)
        #   "SomePayload.registerPayloads("   (direct call)
        foreach ($m in [regex]::Matches($code, '([A-Za-z0-9_]+Payload)\s*::\s*registerPayloads\b')) {
            $payloadTypes[$m.Groups[1].Value] = $rel
        }
        foreach ($m in [regex]::Matches($code, '([A-Za-z0-9_]+Payload)\s*\.\s*registerPayloads\s*\(')) {
            $payloadTypes[$m.Groups[1].Value] = $rel
        }
        # Any register/registerOn call whose receiver looks like a core Layer* registry must not
        # exist here.  registerAll( is deliberately NOT matched: that is the sanctioned per-layer
        # creative-tab API (each layer registers its own tabs into the already-attached register).
        foreach ($m in [regex]::Matches($code, '(?<![A-Za-z0-9_])Layer[A-Za-z0-9_]*\s*\.\s*register(?:On)?\s*\(')) {
            $layerRegisterSites = $layerRegisterSites + ($rel + ': ' + $m.Value)
        }
        # Informational: module-owned registrations of a DeferredRegister onto the mod bus.
        foreach ($m in [regex]::Matches($code, '(?<![A-Za-z0-9_])([A-Z][A-Z0-9_]{2,})\s*\.\s*register\s*\(\s*modEventBus\s*\)')) {
            $deferredRegisterSites = $deferredRegisterSites + ($rel + ': ' + $m.Groups[1].Value)
        }
    }

    Write-Check ($ensureHits.Count -ge 1) ($name + ' D1 references LayerBootstrap.ensureAttached(') `
        ("sites=" + $ensureHits.Count + " [" + ($ensureHits -join ', ') + "]")

    $forbiddenTotal = 0
    $forbiddenDetail = @()
    foreach ($needle in $sharedWiringNames) {
        $hits = @($forbiddenHits[$needle])
        $forbiddenTotal = $forbiddenTotal + $hits.Count
        if ($hits.Count -gt 0) { $forbiddenDetail = $forbiddenDetail + ($needle + ' -> ' + ($hits -join ',')) }
    }
    Write-Check ($forbiddenTotal -eq 0) ($name + ' D2 no direct touch of the shared wiring (core LayerBootstrap owns it)') `
        $(if ($forbiddenTotal -eq 0) { 'checked: ' + ($sharedWiringNames -join ', ') } else { $forbiddenDetail -join '; ' })

    $payloadOk = $true
    $payloadDetail = @()
    foreach ($t in $payloadTypes.Keys) {
        $owner = @($files | Where-Object { $_.Name -eq ($t + '.java') })
        if ($owner.Count -eq 0) { $payloadOk = $false; $payloadDetail = $payloadDetail + ($t + ' (declared in ' + $payloadTypes[$t] + ') has no source file in this module') }
    }
    Write-Check $payloadOk ($name + ' D3 every payload this module registers is declared inside this module') `
        $(if ($payloadTypes.Count -eq 0) { 'no registerPayloads sites in this module' } else { 'types=[' + (($payloadTypes.Keys | Sort-Object) -join ',') + '] ' + ($payloadDetail -join '; ') })

    Write-Check ($layerRegisterSites.Count -eq 0) ($name + ' D4 no module touches a core Layer* register directly') `
        $(if ($layerRegisterSites.Count -eq 0) { '0 LayerXxx.register* sites' } else { $layerRegisterSites -join '; ' })

    Write-Host ("        info  D5 module-owned DeferredRegister-style .register( sites: " + $deferredRegisterSites.Count + `
        $(if ($deferredRegisterSites.Count -gt 0) { ' [' + ($deferredRegisterSites -join '; ') + ']' } else { '' }))
}
Write-Host ''

# ---------------------------------------------------------------------------
# E. hand-written data/** home assertions (P7c)
#    The root project (mod id coe_integration) is the integration layer and is not the shipped
#    artifact: the three module jars are.  Before P7c all 27 hand-written data files lived in
#    src/main/resources/data, so a player installing coe.jar (or cews.jar / transmutation.jar)
#    got NONE of them -- silently.  These two assertions pin the fixed shape: a module's own
#    data files travel in that module's jar, and nothing is left behind in the root tree.
# ---------------------------------------------------------------------------
Write-Host '[E] hand-written data/** home assertions'
foreach ($name in $modules.Keys) {
    $jarPath = Join-Path $repoRoot $modules[$name].jar
    $entries = @(Get-JarEntries $jarPath)
    $ownDataRel = $name + '\src\main\resources\data'
    $ownDataRoot = Join-Path $repoRoot $ownDataRel
    $ownFiles = @()
    if (Test-Path -LiteralPath $ownDataRoot) {
        $ownFiles = @(Get-ChildItem -LiteralPath $ownDataRoot -Recurse -File -Filter '*.json')
    }
    $ownMissing = @()
    $ownMismatch = @()
    foreach ($of in $ownFiles) {
        $entry = Get-DataJarEntry $of.FullName
        if (-not $entry) { continue }
        if (-not ($entries -contains $entry)) { $ownMissing = $ownMissing + $entry; continue }
        if ((Get-JarEntryHash $jarPath $entry) -ne (Get-FileHash -LiteralPath $of.FullName -Algorithm SHA256).Hash) {
            $ownMismatch = $ownMismatch + $entry
        }
    }
    Write-Check (($ownMissing.Count -eq 0) -and ($ownMismatch.Count -eq 0)) `
        ($name + ' E1 every hand-written data/** file of this module is packaged in its own jar') `
        ("ownHome=" + $ownDataRel + " ownFiles=" + $ownFiles.Count + " missing=" + $ownMissing.Count + " hashMismatch=" + $ownMismatch.Count + `
         $(if ($ownMissing.Count -gt 0) { ' missing=[' + (($ownMissing | Select-Object -First 8) -join ', ') + $(if ($ownMissing.Count -gt 8) { ', ...' } else { '' }) + ']' } else { '' }))
}

$rootDataRel = 'src\main\resources\data'
$rootData = Join-Path $repoRoot $rootDataRel
$stranded = @()
if (Test-Path -LiteralPath $rootData) {
    $stranded = @(Get-ChildItem -LiteralPath $rootData -Recurse -File)
}
Write-Check ($stranded.Count -eq 0) 'E2 no hand-written data/** file is stranded in the root integration layer' `
    ("rootHome=" + $rootDataRel + " files=" + $stranded.Count + `
     $(if ($stranded.Count -gt 0) { ' -> move each one to core/src/main/resources/data (vanilla/external or cross-layer ids) or to the module that owns its ids: [' + (($stranded | ForEach-Object { $_.FullName.Substring($rootData.Length + 1) }) -join ', ') + ']' } else { ' (root does not publish, so a file left here reaches no shipped jar)' }))
Write-Host ''

# ---------------------------------------------------------------------------
# summary
# ---------------------------------------------------------------------------
Write-Host ("checks run: " + $script:checkCount + ", failed: " + $script:failures.Count)
if ($script:failures.Count -eq 0) {
    Write-Host 'OK: every module jar is self-sufficient for the machinery this script knows how to see.'
    exit 0
}
Write-Host 'FAILED assertions:'
foreach ($f in $script:failures) { Write-Host ('  - ' + $f) }
exit 1
