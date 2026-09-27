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
#      B3 every @JeiPlugin class the module's SOURCE declares is packaged in that module's
#         jar -- and no plugin is packaged that the source does not declare.  Confirmed with
#         javap -v (the annotation is RuntimeInvisible, so "the source says @JeiPlugin" is
#         not the same evidence as "the built class carries it").
#         W6-b2 (2026-09-29) made this two-sided: it used to read ">=1 @JeiPlugin", which was
#         true while every shipped jar held content.  The third layer is now an empty shell
#         (its only plugin, transmutation_jei, moved into :coe together with the whole
#         transmutation mechanism), so ">=1" would demand a plugin that has no reason to
#         exist.  Comparing source-declared vs jar-packaged keeps the original failure mode
#         (a plugin whose class did not ship -- the P7a shape) AND catches a stray plugin,
#         with the same one check per module (was 66 in total; W6-c added X2, so it is 67).
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
#      Comments are stripped before matching D1-D4 (and before counting @JeiPlugin for B3),
#      so javadoc that merely mentions one of these names does not count as a call site.
#      W6-d (2026-09-30): the strip used to remove BLOCK comments first and LINE comments
#      second, and that order silently swallowed real code -- see Get-SourceCode and the
#      "W6-d SILENT-HOLE" note there.  It is now one left-to-right pass.  The strip is still
#      deliberately naive about STRING literals (it cuts at "//" inside one); that can only
#      hide a violation sitting after such a literal on the same line, which none of these
#      patterns can be.
#   E. hand-written data/** home assertions (P7c):
#      E1 every file under <module>/src/main/resources/data/** is packaged in that module's own
#         jar, byte-identical.  This is what makes the per-layer half of the P7c move real.
#      E2 src/main/resources/data/** holds no file at all.  The root project (coe_integration)
#         is the integration layer and is NOT shipped, so a data file left there reaches no
#         module jar; every one of them belongs in core (shared) or in its owning module.
#         This is the assertion that was red before P7c (27 stranded files) and is the durable
#         form of that hole -- it cannot come back by someone re-adding a file to the root tree.
#   F. generated recipe distribution (P7d).  Before P7d all 221 generated recipes
#      (61 COE dismantling + 160 CEWS tool-charging) were written to the ROOT datagen output
#      only: the three module jars held 111 unrelated hand-written recipes and had an
#      intersection of ZERO with those 221.  A player installing cews.jar alone got none of
#      the 160 tool-charging recipes; installing coe.jar alone got none of the 61 dismantling
#      recipes.  The layer is bound at the Coe/CewsRecipeProvider call site (never inferred
#      from the path -- both layers write data/createoreexpansion/recipe/...), so these
#      assertions pin the distribution:
#      F1 src/generated/resources/data/**/recipe/** holds no file at all (same durable shape
#         as E2: the root output is not a shipped artifact).
#      F2 the per-module generated recipe counts are exactly coe=221 / cews=0 /
#         transmutation=0 and their sum is 221 (W6-c: the 160 tool-charging recipes are :coe's
#         now, because their recipe type and the three stress chargers moved into the first
#         layer).  Adding or removing a generated recipe turns the gate red on purpose;
#         update this table together with the recipe.
#      F3 every generated recipe file of a module is packaged in THAT module's jar,
#         byte-identical (SHA-256).
#      F4 none of a module's generated recipes appears in another module's jar: the explicit
#         per-provider layer binding must not leak across layers.  W6-c sharpened what that
#         means: "the 160 tool-charging recipes must never appear in cews.jar" is exactly this
#         leak (a stale "cews" layer binding in LayerRecipeRouter would put all 160 there).
#   G. hand-written block tags, split per layer (P7d).
#      data/createoreexpansion/tags/block/jade_sapphire_light.json (17 COE + 1 CEWS) and
#      machines_heavy.json (1 COE + 2 CEWS) used to be carried WHOLE by every module jar (they
#      lived in the shared home).  TagLoader (L45-59) accumulates every copy of the same
#      data-pack path, but a single-module install then references blocks that jar does not
#      contain and TagLoader drops the WHOLE tag with an ERROR (L84-115) -- a fake
#      self-sufficiency.  The fix is the P4f shape: the same path split into one file per
#      layer, each holding only that layer's entries, so the union at runtime is unchanged.
#      W6-c: jade_sapphire_light is no longer split -- the single CEWS element
#      (jade_stress_charger) moved into :coe with the chargers, so the COE half now carries the
#      whole 18-element pre-split set (charger at index 4) and the cews half file is gone.  A
#      layer with no half must not carry the file at all.
#      G1 neither split tag remains in the shared home (that is what made every jar carry the
#         whole file).
#      G2 every expected half exists, and the union of their values is element-for-element the
#         pre-split set (nothing added, nothing removed); each half is pinned in order, and a
#         layer with no half is asserted NOT to carry the file.
#      G3 a module jar carries exactly its own half of each split tag and never the other
#         layer's half.
#   X2. the "which recipe types a wave may process, in which order" list is frozen.
#      WaveRecipeCapabilities.all() decides the order in which a stellar wave picks its recipe;
#      it is player-visible and invisible to every other gate (a reordered .add(...) compiles,
#      passes runData and passes every layering/package assertion -- W6-b2 hit exactly this).
#      This check rebuilds the expansion from the sources (LayerOrder declaration order, then
#      per-weight .add(...) argument order, deduplicated by recipe-type id) and compares it
#      element for element with a fixed 6-entry table.
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
# genRecipes is the number of datagen-produced recipes (P7d) this module must
# carry under <module>/src/generated/resources/data/**/recipe/**.
# W6-c (2026-09-29) turned the old 61 (COE) + 160 (CEWS) split into a single
# 221-strong COE set: the 160 tool-charging recipes (32 chargeable items x 5
# levels) are produced by :coe now, because their recipe type
# (createoreexpansion:charging) and the three stress chargers moved into the
# first layer.  cews must therefore hold ZERO of them -- "the 160 must never
# appear in cews.jar" is the new, sharper shape of assertion F4.
# ---------------------------------------------------------------------------
$modules = [ordered]@{
    'coe' = @{
        jar        = 'coe\build\libs\createoreexpansion-1.0.0.jar'
        modId      = 'createoreexpansion'
        src        = 'coe\src\main\java'
        genRecipes = 221
    }
    'cews' = @{
        jar        = 'cews\build\libs\cews-1.0.0.jar'
        modId      = 'cews'
        src        = 'cews\src\main\java'
        genRecipes = 0
    }
    'transmutation' = @{
        jar        = 'transmutation\build\libs\transmutation-1.0.0.jar'
        modId      = 'transmutation'
        src        = 'transmutation\src\main\java'
        genRecipes = 0
    }
}

$expectedGeneratedRecipes = 221

# P7d: the hand-written block tags that referenced BOTH layers.  Each layer owns a half at
# the SAME data-pack path in its own jar; TagLoader merges the copies at runtime.  'full' is
# the pre-split element list: the halves must cover exactly it (G2).
#
# W6-c (2026-09-29): jade_sapphire_light is no longer split.  Its only CEWS element was
# createoreexpansion:jade_stress_charger, and that block moved into :coe with the chargers,
# so the COE half now carries the whole pre-split set (18 elements, jade_stress_charger at
# index 4 = right after raw_jade_block, exactly where it sat in the pre-split file) and the
# CEWS half file was deleted.  A layer whose expected half is ABSENT must not carry the file
# at all -- G2 and G3 both assert that, which is strictly stronger than the old "half exists
# with 0 values" shape.
$splitTags = [ordered]@{
    'jade_sapphire_light' = @{
        full = @(
            'createoreexpansion:jade_block',
            'createoreexpansion:jade_ore',
            'createoreexpansion:deepslate_jade_ore',
            'createoreexpansion:raw_jade_block',
            'createoreexpansion:jade_stress_charger',
            'createoreexpansion:jade_small_bud',
            'createoreexpansion:jade_medium_bud',
            'createoreexpansion:jade_large_bud',
            'createoreexpansion:jade_cluster',
            'createoreexpansion:jade_budding_block',
            'createoreexpansion:sapphire_block',
            'createoreexpansion:nether_sapphire_ore',
            'createoreexpansion:raw_sapphire_block',
            'createoreexpansion:sapphire_small_bud',
            'createoreexpansion:sapphire_medium_bud',
            'createoreexpansion:sapphire_large_bud',
            'createoreexpansion:sapphire_cluster',
            'createoreexpansion:sapphire_budding_block'
        )
        coe  = @(
            'createoreexpansion:jade_block',
            'createoreexpansion:jade_ore',
            'createoreexpansion:deepslate_jade_ore',
            'createoreexpansion:raw_jade_block',
            'createoreexpansion:jade_stress_charger',
            'createoreexpansion:jade_small_bud',
            'createoreexpansion:jade_medium_bud',
            'createoreexpansion:jade_large_bud',
            'createoreexpansion:jade_cluster',
            'createoreexpansion:jade_budding_block',
            'createoreexpansion:sapphire_block',
            'createoreexpansion:nether_sapphire_ore',
            'createoreexpansion:raw_sapphire_block',
            'createoreexpansion:sapphire_small_bud',
            'createoreexpansion:sapphire_medium_bud',
            'createoreexpansion:sapphire_large_bud',
            'createoreexpansion:sapphire_cluster',
            'createoreexpansion:sapphire_budding_block'
        )
    }
    'machines_heavy' = @{
        full = @(
            'createoreexpansion:power_angle_grinder',
            'createoreexpansion:energy_wave_regulator',
            'createoreexpansion:wave_speed_regulator'
        )
        coe  = @('createoreexpansion:power_angle_grinder')
        cews = @(
            'createoreexpansion:energy_wave_regulator',
            'createoreexpansion:wave_speed_regulator'
        )
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
#
# W6-d (2026-09-30): the CALLER must now assert that this returned a record for every class
# it asked about (B6a / C2a).  The parse loop below is keyed by the "Classfile " line javap
# prints per class; a class for which javap prints no such line (error, unreadable entry,
# unsupported class file version on some JDK) simply gets NO record -- and a class with no
# record is a class B6 never judged and, in the core jar, a class C2 would count as "not a
# subscriber" while never having looked at it.  That is the exact failure shape this whole
# script exists to catch (an assertion that passes because it saw nothing), so the count is
# asserted instead of assumed.
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
#
# W6-d SILENT-HOLE FIX (2026-09-30).  The old body was:
#     $raw = [regex]::Replace($raw, '(?s)/\*.*?\*/', '')   # block comments FIRST
#     $raw = [regex]::Replace($raw, '(?m)//.*$', '')       # line comments SECOND
# That order is WRONG, and it fails in the quiet direction.  If a LINE comment contains
# "/*" -- a completely ordinary thing to write, e.g. "// see content/energyfield/**" --
# the block pass pairs that "/*" with the NEXT "*/" anywhere later in the file and deletes
# everything in between, real code included.  Measured on this tree (W6-c): it ate the
#     modEventBus.addListener(EnergyFieldSyncPayload::registerPayloads);
# line in CreateOreExpansion.java, so D3 reported ONE payload type instead of two and
# STILL printed PASS -- an assertion that "worked" while unable to see the site it exists
# for.  The mirror case is just as bad: a "//" inside a block comment makes the line pass
# delete the "*/" that closes it, which leaves an unterminated "/*" behind that can pair
# with a LATER "*/" and swallow the code between -- so neither sequential order is safe.
#
# The replacement is ONE left-to-right pass with an alternation, i.e. the exact semantics
# "strip whichever comment starts first":
#     (?s:/\*.*?\*/)   a block comment, closed at its FIRST "*/"
#     (?m://.*$)       a line comment, to the end of the line
# .NET scans left to right and takes the LEFTMOST match, so a "/*" inside a line comment
# can no longer open a block, and a "//" inside a block comment can no longer close or
# merge one (the whole block is consumed by a single match before the scanner ever reaches
# the "//").  This is a STRICTENING: the visible-text set is a superset of what EITHER
# sequential order produced for real code, so it can only ever UN-hide a call site.  There
# is no path by which it hides something the old body saw.
#
# Remaining, documented imprecision (unchanged from before): a "//" inside a STRING literal
# still cuts the rest of that line.  Closing that needs a real tokenizer (string/char
# literals, text blocks, escapes); it is deliberately left alone here because a buggy
# tokenizer is a much worse failure mode than this known, one-line-bounded one.
function Get-SourceCode {
    param([string]$Path)
    $raw = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
    $raw = [regex]::Replace($raw, '(?s:/\*.*?\*/)|(?m://.*$)', '')
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

    # B6a -- the parser must not have silently skipped input (see Get-CompiledAnnotations).
    Write-Check ($records.Count -eq $classNames.Count) `
        ($name + ' B6a javap produced a record for every class of this jar') `
        ("classesInJar=" + $classNames.Count + " parsedRecords=" + $records.Count + `
         $(if ($records.Count -ne $classNames.Count) { ' -> a class with no record is a class B6 never judged' } else { ' (no class was skipped by the annotation parser)' }))

    $jeiClasses = @()
    foreach ($cls in $records.Keys) {
        if ($records[$cls].Jei -and ($cls -like '*compat/jei/*') -and ($cls -like '*!/com/hjmmd_8/createoreexpansion/*')) {
            $jeiClasses = $jeiClasses + $cls
        }
    }
    # W6-b2: two-sided (see the header).  The expected number is what the module's own SOURCE
    # declares, so an empty module (the third layer's shell) legitimately expects 0.
    $jeiSrcRoot = Join-Path $repoRoot $modules[$name].src
    $jeiSrcCount = 0
    if (Test-Path -LiteralPath $jeiSrcRoot) {
        foreach ($jf in (Get-ChildItem -LiteralPath $jeiSrcRoot -Recurse -File -Filter '*.java')) {
            $jeiSrcCount += @([regex]::Matches((Get-SourceCode $jf.FullName), '@JeiPlugin\b')).Count
        }
    }
    Write-Check ($jeiClasses.Count -eq $jeiSrcCount) ($name + ' B3 packaged @JeiPlugin count == source-declared count (javap -v)') `
        ("jar=" + $jeiClasses.Count + " source=" + $jeiSrcCount + " -> " + `
         $(if ($jeiClasses.Count -gt 0) { (($jeiClasses | ForEach-Object { ($_ -split '!/')[-1] }) -join ', ') } else { '(none declared, none packaged)' }))

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
Write-Check ($coreRecords.Count -eq $coreClassNames.Count) `
    'core C2a javap produced a record for every class of the library jar' `
    ("classesInJar=" + $coreClassNames.Count + " parsedRecords=" + $coreRecords.Count + `
     $(if ($coreRecords.Count -ne $coreClassNames.Count) { ' -> a class with no record is treated as "no subscriber" without ever being read' } else { ' (no class was skipped by the annotation parser)' }))
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
# F. generated recipe distribution (P7d)
#    Before P7d every one of the 221 datagen-produced recipes (61 COE dismantling + 160 CEWS
#    tool-charging) was written to the ROOT output only, so a module jar held ZERO of them.
#    The layer is bound at the Coe/CewsRecipeProvider call site (never inferred from the path:
#    both layers write data/createoreexpansion/recipe/...), so these assertions pin where each
#    layer's recipes ended up.
# ---------------------------------------------------------------------------
Write-Host '[F] generated recipe distribution (P7d)'

$recipeEntryPattern = '^data/[^/]+/recipe/.+\.json$'

# One jar listing per module, reused by F3 and F4.
$moduleEntries = @{}
foreach ($name in $modules.Keys) {
    $moduleEntries[$name] = @(Get-JarEntries (Join-Path $repoRoot $modules[$name].jar))
}

function Get-GeneratedRecipes {
    param([string]$DataRoot)
    $found = @()
    if (-not (Test-Path -LiteralPath $DataRoot)) { return $found }
    foreach ($f in (Get-ChildItem -LiteralPath $DataRoot -Recurse -File -Filter '*.json')) {
        $entry = Get-DataJarEntry $f.FullName
        if ($entry -and ($entry -match $recipeEntryPattern)) { $found = $found + $f }
    }
    return $found
}

# F1 -- the root datagen output must not hold generated recipes any more (E2's twin).
$rootRecipes = @(Get-GeneratedRecipes (Join-Path $repoRoot 'src\generated\resources\data'))
Write-Check ($rootRecipes.Count -eq 0) 'F1 no generated recipe is stranded in the root datagen output' `
    ("rootData=src\generated\resources\data files=" + $rootRecipes.Count + `
     $(if ($rootRecipes.Count -gt 0) { ' -> the layer binding is not active; every recipe must be written to the module that produced it: [' + (($rootRecipes | Select-Object -First 5 | ForEach-Object { Get-DataJarEntry $_.FullName }) -join ', ') + $(if ($rootRecipes.Count -gt 5) { ', ...' } else { '' }) + ']' } else { ' (root does not publish, so a recipe left here reaches no shipped jar)' }))

# F2 -- exact per-module counts and their sum.
$ownRecipes = @{}
$recipeTotal = 0
$recipeDetail = @()
$countsOk = $true
foreach ($name in $modules.Keys) {
    $ownRecipes[$name] = @(Get-GeneratedRecipes (Join-Path $repoRoot ($name + '\src\generated\resources\data')))
    $recipeTotal = $recipeTotal + $ownRecipes[$name].Count
    if ($ownRecipes[$name].Count -ne $modules[$name].genRecipes) { $countsOk = $false }
    $recipeDetail = $recipeDetail + ($name + '=' + $ownRecipes[$name].Count + '/' + $modules[$name].genRecipes)
}
$countsOk = $countsOk -and ($recipeTotal -eq $expectedGeneratedRecipes)
Write-Check $countsOk 'F2 the modules hold exactly the expected generated recipe counts (sum 221)' `
    ("[" + ($recipeDetail -join '  ') + "] sum=" + $recipeTotal + " expectedSum=" + $expectedGeneratedRecipes + `
     " (W6-c: 221 = COE dismantling 61 + COE tool-charging 160; cews must hold 0 of them)")

# F3 -- each module packages its own generated recipes, byte for byte.
foreach ($name in $modules.Keys) {
    $jarpath = Join-Path $repoRoot $modules[$name].jar
    $missing = @()
    $mismatch = @()
    foreach ($f in $ownRecipes[$name]) {
        $entry = Get-DataJarEntry $f.FullName
        if (-not $entry) { continue }
        if (-not ($moduleEntries[$name] -contains $entry)) { $missing = $missing + $entry; continue }
        if ((Get-JarEntryHash $jarpath $entry) -ne (Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash) {
            $mismatch = $mismatch + $entry
        }
    }
    $nonEmpty = ($modules[$name].genRecipes -eq 0) -or ($ownRecipes[$name].Count -gt 0)
    Write-Check (($missing.Count -eq 0) -and ($mismatch.Count -eq 0) -and $nonEmpty) `
        ($name + ' F3 every generated recipe of this module is packaged in its own jar byte-identical') `
        ("ownGenerated=" + $ownRecipes[$name].Count + " missing=" + $missing.Count + " hashMismatch=" + $mismatch.Count + `
         $(if ($missing.Count -gt 0) { ' missing=[' + (($missing | Select-Object -First 5) -join ', ') + $(if ($missing.Count -gt 5) { ', ...' } else { '' }) + ']' } else { '' }))
}

# F4 -- the explicit layer binding must not leak: no generated recipe of this module turns up
# in a foreign jar.  W6-c sharpened what this means for the 160 tool-charging recipes: they are
# :coe's now, so "they must never appear in cews.jar" is exactly the leak this check catches
# (a stale "cews" binding in LayerRecipeRouter would put all 160 there and turn this red).
foreach ($name in $modules.Keys) {
    $leaks = @()
    $chargingLeaks = @()
    foreach ($other in $modules.Keys) {
        if ($other -ceq $name) { continue }
        foreach ($f in $ownRecipes[$name]) {
            $entry = Get-DataJarEntry $f.FullName
            if ($entry -and ($moduleEntries[$other] -contains $entry)) {
                $leaks = $leaks + ($entry + ' -> ' + $other + '.jar')
                if ($entry -match '/recipe/tool_charge/') { $chargingLeaks = $chargingLeaks + $entry }
            }
        }
    }
    Write-Check ($leaks.Count -eq 0) ($name + ' F4 no generated recipe of this module appears in another module jar') `
        ("ownGenerated=" + $ownRecipes[$name].Count + " leaked=" + $leaks.Count + " toolChargingLeaked=" + $chargingLeaks.Count + $(if ($leaks.Count -gt 0) { ' [' + (($leaks | Select-Object -First 5) -join ', ') + ']' } else { '' }))
}
Write-Host ''

# ---------------------------------------------------------------------------
# G. cross-layer hand-written block tags split per layer (P7d)
#    jade_sapphire_light (17 COE + 1 CEWS) and machines_heavy (1 COE + 2 CEWS) used to live in the
#    shared home, which every module jar carries WHOLE.  On a single-module install the references
#    to the other layer's blocks cannot resolve, and TagLoader L84-115 then logs an ERROR and drops
#    the entire tag -- so "shared everywhere" was a fake self-sufficiency.  Each tag is now one
#    file per layer at the same data-pack path; TagLoader unions the copies at runtime (L45-59,
#    replace=false), so the merged result is the pre-split set.
# ---------------------------------------------------------------------------
Write-Host '[G] cross-layer hand-written block tags split per layer (P7d)'

$tagSourceRel = 'src\main\resources\data\createoreexpansion\tags\block'
$sharedHomeAbs = Join-Path $repoRoot 'core\src\main\resources\data'

$leftover = @()
foreach ($tagName in $splitTags.Keys) {
    if (Test-Path -LiteralPath (Join-Path $sharedHomeAbs ('createoreexpansion\tags\block\' + $tagName + '.json'))) {
        $leftover = $leftover + $tagName
    }
}
Write-Check ($leftover.Count -eq 0) 'G1 neither split tag is left in the shared data home' `
    ("sharedHome=core\src\main\resources\data leftover=[" + ($leftover -join ', ') + "]" + `
     $(if ($leftover.Count -gt 0) { ' (a whole file there is copied into EVERY module jar, and a single-module install then fails the tag with an ERROR)' } else { '' }))

foreach ($tagName in $splitTags.Keys) {
    $spec = $splitTags[$tagName]
    $problems = @()
    $seen = @()
    # W6-c: a layer with NO half (key absent, or an empty array) must not carry the file at
    # all.  That covers the old split shape (both halves exist, each non-empty) and the new
    # "the whole tag belongs to one layer" shape, and it is strictly stronger than the old
    # "file must exist with 0 values" reading.
    foreach ($layer in @('coe', 'cews')) {
        $rel = 'data\createoreexpansion\tags\block\' + $tagName + '.json'
        $file = Join-Path $repoRoot ($layer + '\' + $tagSourceRel + '\' + $tagName + '.json')
        $expected = @()
        if ($spec.ContainsKey($layer)) { $expected = @($spec[$layer]) }
        if ($expected.Count -eq 0) {
            if (Test-Path -LiteralPath $file) { $problems = $problems + ($layer + ': carries a half this layer does not own') }
            continue
        }
        if (-not (Test-Path -LiteralPath $file)) { $problems = $problems + ($layer + ': file missing'); continue }
        $obj = $null
        try { $obj = (Get-Content -LiteralPath $file -Raw -Encoding UTF8) | ConvertFrom-Json } catch { $problems = $problems + ($layer + ': unparsable json'); continue }
        if ($obj.replace -ne $false) { $problems = $problems + ($layer + ': replace is not false') }
        $values = @($obj.values)
        if ($values.Count -ne $expected.Count) { $problems = $problems + ($layer + ': values=' + $values.Count + ' expected=' + $expected.Count) }
        for ($i = 0; $i -lt [Math]::Min($values.Count, $expected.Count); $i++) {
            if ([string]$values[$i] -cne [string]$expected[$i]) {
                $problems = $problems + ($layer + ' value[' + $i + ']=' + $values[$i] + ' expected=' + $expected[$i])
            }
        }
        $seen = $seen + $values
    }
    $full = @($spec.full)
    if ($seen.Count -ne $full.Count) { $problems = $problems + ('union=' + $seen.Count + ' presplit=' + $full.Count) }
    foreach ($v in $full) { if ($seen -notcontains $v) { $problems = $problems + ('lost ' + $v) } }
    foreach ($v in $seen) { if ($full -notcontains $v) { $problems = $problems + ('added ' + $v) } }
    $coeCount = if ($spec.ContainsKey('coe')) { @($spec.coe).Count } else { 0 }
    $cewsCount = if ($spec.ContainsKey('cews')) { @($spec.cews).Count } else { 0 }
    Write-Check ($problems.Count -eq 0) ('G2 ' + $tagName + ' halves exist and their union is element-for-element the pre-split set') `
        $(if ($problems.Count -eq 0) { 'coe=' + $coeCount + ' cews=' + $cewsCount + ' union=' + $full.Count + ' presplit=' + $full.Count + ' lost=0 added=0' } else { $problems -join '; ' })
}

foreach ($name in $modules.Keys) {
    $jarpath = Join-Path $repoRoot $modules[$name].jar
    $problems = @()
    foreach ($tagName in $splitTags.Keys) {
        $spec = $splitTags[$tagName]
        $entry = 'data/createoreexpansion/tags/block/' + $tagName + '.json'
        $rel = 'data\createoreexpansion\tags\block\' + $tagName + '.json'
        $inJar = ($moduleEntries[$name] -contains $entry)
        # W6-c: ownership is "this layer has a NON-EMPTY half", not merely "the key exists".
        $owns = $spec.ContainsKey($name) -and (@($spec[$name]).Count -gt 0)
        if ($owns) {
            $file = Join-Path $repoRoot ($name + '\' + $tagSourceRel + '\' + $tagName + '.json')
            if (-not (Test-Path -LiteralPath $file)) { $problems = $problems + ($tagName + ': own half source missing'); continue }
            if (-not $inJar) { $problems = $problems + ($tagName + ': own half not in jar'); continue }
            if ((Get-JarEntryHash $jarpath $entry) -ne (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash) {
                $problems = $problems + ($tagName + ': own half is not the packaged bytes')
            }
        } elseif ($inJar) {
            $problems = $problems + ($tagName + ': carries a half this layer does not own')
        }
    }
    Write-Check ($problems.Count -eq 0) ($name + ' G3 this jar carries exactly its own half of every split tag') `
        $(if ($problems.Count -eq 0) { 'splitTags=' + $splitTags.Count + ' both checked' } else { $problems -join '; ' })
}
Write-Host ''

# ---------------------------------------------------------------------------
# X2. the "which recipe types a wave may process, in which order" list is FROZEN (W6-c).
#
#     WaveRecipeCapabilities.all() drives the ORDER in which a stellar wave picks the recipe
#     it will process.  That order is player-visible, and NONE of the other gates can see it:
#     a re-ordered .add(...) still compiles, still passes runData, and still passes every
#     layering / package assertion.  W6-b2 hit exactly this class of bug (a forwarding field
#     pulled charging to numeric registry id 8 instead of 12; only a probe that printed the
#     numeric ids caught it).
#
#     This check reconstructs the expansion from the SOURCE, the same way the runtime builds
#     it, and compares it element for element with a frozen 6-entry table:
#       order = LayerOrder enum declaration order (weight),
#       then, inside each weight, the .add(...) argument order as written,
#       deduplicated by recipe-type id (vanilla Set.add), nulls/ids ignored.
#     Anything that changes the list -- reordering the enum, reordering an .add(...), moving a
#     type to another weight, dropping one, or adding an addOther(...) tail -- turns this red.
# ---------------------------------------------------------------------------
Write-Host '[X2] wave-recipe capability order is frozen (source reconstruction vs fixed table)'

$waveCapsRel  = 'core\src\main\java\com\hjmmd_8\createoreexpansion\common\registry\WaveRecipeCapabilities.java'
$waveCapsPath = Join-Path $repoRoot $waveCapsRel
$expectedWaveOrder = @('transmuting', 'lightning', 'lightning_block', 'grinding', 'dismantling', 'charging')

$x2Problems = @()
$orderNames = @()
$fieldIds = @{}
$registrations = @()

if (-not (Test-Path -LiteralPath $waveCapsPath)) {
    $x2Problems = $x2Problems + ('missing ' + $waveCapsRel)
} else {
    $caps = Get-SourceCode $waveCapsPath
    $enumMatch = [regex]::Match($caps, 'enum\s+LayerOrder\s*\{([\s\S]*?)\}')
    if (-not $enumMatch.Success) {
        $x2Problems = $x2Problems + 'cannot parse the LayerOrder enum'
    } else {
        foreach ($em in [regex]::Matches($enumMatch.Groups[1].Value, '(?m)^\s*([A-Z][A-Z0-9_]*)\s*,?\s*$')) {
            $orderNames = $orderNames + $em.Groups[1].Value
        }
        if ($orderNames.Count -eq 0) { $x2Problems = $x2Problems + 'LayerOrder enum lists no constants' }
    }
}

$waveSrcRoots = @(
    (Join-Path $repoRoot 'src\main\java\com\hjmmd_8\createoreexpansion'),
    (Join-Path $repoRoot 'core\src\main\java\com\hjmmd_8\createoreexpansion'),
    (Join-Path $repoRoot 'coe\src\main\java\com\hjmmd_8\createoreexpansion'),
    (Join-Path $repoRoot 'cews\src\main\java\com\hjmmd_8\createoreexpansion'),
    (Join-Path $repoRoot 'transmutation\src\main\java\com\hjmmd_8\createoreexpansion')
)
foreach ($root in $waveSrcRoots) {
    if (-not (Test-Path -LiteralPath $root)) { continue }
    foreach ($file in @(Get-ChildItem -LiteralPath $root -Recurse -File -Filter '*.java' | Sort-Object FullName)) {
        $code = Get-SourceCode $file.FullName
        # recipe-type constants: public static final LayerRecipeType NAME = LayerRecipeType.processing|serializer("ID", ...)
        foreach ($fm in [regex]::Matches($code, 'static\s+final\s+LayerRecipeType\s+([A-Za-z0-9_]+)\s*=\s*LayerRecipeType\s*\.\s*(?:processing|serializer)\s*\(\s*"([A-Za-z0-9_]+)"')) {
            $fieldIds[$fm.Groups[1].Value] = $fm.Groups[2].Value.ToLowerInvariant()
        }
        # registration chains: addOrdered(LayerOrder.X) followed by one or more .add(f1, f2, ...)
        foreach ($rm in [regex]::Matches($code, 'addOrdered\s*\(\s*(?:[A-Za-z0-9_.]*\.)?LayerOrder\s*\.\s*([A-Za-z0-9_]+)\s*\)')) {
            $orderName = $rm.Groups[1].Value
            $pos = $rm.Index + $rm.Length
            while ($true) {
                $add = [regex]::Match($code.Substring($pos), '^\s*\.\s*add\s*\(([^)]*)\)')
                if (-not $add.Success) { break }
                $fields = @($add.Groups[1].Value -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' })
                $registrations = $registrations + @{ Order = $orderName; Fields = $fields }
                $pos = $pos + $add.Length
            }
        }
        # addOther(...) appends to the UNWEIGHTED tail of all(); it is not part of the frozen
        # table, so a call to it means this check can no longer prove the whole sequence.
        # The declaration itself ("public static void addOther(...)") is not a call.
        if ([regex]::IsMatch($code, '(?<!void )addOther\s*\(')) {
            $x2Problems = $x2Problems + ('unexpected addOther(...) registration in ' + $file.Name + ' -- the frozen tail of all() cannot be checked')
        }
    }
}
if ($registrations.Count -eq 0) { $x2Problems = $x2Problems + 'no WaveRecipeCapabilities.addOrdered(...) registration found in any source root' }

$waveExpansion = @()
foreach ($orderName in $orderNames) {
    $bucket = @()
    foreach ($reg in $registrations) {
        if ($reg.Order -ne $orderName) { continue }
        foreach ($fieldName in $reg.Fields) {
            if (-not $fieldIds.ContainsKey($fieldName)) {
                $x2Problems = $x2Problems + ('registration names an unknown recipe-type field: ' + $fieldName)
                continue
            }
            $typeId = $fieldIds[$fieldName]
            if ($bucket -notcontains $typeId) { $bucket = $bucket + $typeId }
        }
    }
    $waveExpansion = $waveExpansion + $bucket
}

if ($waveExpansion.Count -ne $expectedWaveOrder.Count) {
    $x2Problems = $x2Problems + ('expansion has ' + $waveExpansion.Count + ' entries, expected ' + $expectedWaveOrder.Count)
} else {
    for ($i = 0; $i -lt $expectedWaveOrder.Count; $i++) {
        if ([string]$waveExpansion[$i] -cne [string]$expectedWaveOrder[$i]) {
            $x2Problems = $x2Problems + ('order[' + $i + ']=' + $waveExpansion[$i] + ' expected=' + $expectedWaveOrder[$i])
        }
    }
}

Write-Check ($x2Problems.Count -eq 0) 'X2 WaveRecipeCapabilities.all() expands to the frozen order' `
    $(if ($x2Problems.Count -eq 0) {
        'order=' + ($waveExpansion -join ' -> ') + ' (frozen; weights=' + ($orderNames -join ',') + '; registrations=' + $registrations.Count + ')'
    } else {
        'got=' + ($waveExpansion -join ',') + ' expected=' + ($expectedWaveOrder -join ',') + ' :: ' + ($x2Problems -join '; ')
    })
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
