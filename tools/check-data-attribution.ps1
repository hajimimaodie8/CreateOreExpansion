# check-data-attribution.ps1 -- cross-module DATA reference attribution
#   ***OBERVATION ONLY***: this tool's ONLY failure source is the anti-vacuity guard block.
#
# WHY THIS EXISTS
#   The asset half of "who owns what" is already gated by tools/check-asset-attribution.ps1
#   (committed).  The DATA half -- cross-layer references inside data/**/*.json -- had only
#   ever been scanned for two shapes (tags and recipe types).  The reason the obvious
#   upgrade ("reference must resolve inside the referring module or its required closure")
#   was NOT shipped is that the difficulty is not the scan: it is that an
#   "id -> owning module" index does not exist, because a data reference names a REGISTRY
#   ENTRY and a registry entry has no path you can look up.
#
#   So phase 1 is deliberately observation-only:
#     * extract every own-namespace reference (no key whitelist -- every JSON string value
#       is examined, whatever key it sits under),
#     * build the missing index from two independent evidence routes,
#     * report what resolves, what does not, and where the references cross modules,
#     * and FAIL ONLY when the scanner itself matched (nearly) nothing.
#
#   That asymmetry is on purpose.  This tree has already been burned twice by permanently
#   red gates that everyone learned to ignore (check-package-heritage had to be demoted to
#   a report; the first asset-attribution A1 ignored dependency direction and was red on a
#   perfectly legal tree).  A gate that red-flags an unindexed id -- when the index is
#   KNOWN to be incomplete, e.g. Registrate derives "transmutation_fluid_bucket" from
#   ".fluid("transmutation_fluid").bucket()", and CoeItems#grindingWheel builds the item id
#   from a helper argument -- would be exactly that mistake.  Hence: UNINDEXED is printed,
#   never failed.  The index is the discovery goal of this phase, not the gate.
#
# WHAT IT DOES
#   1. CARRIER INVENTORY.  For each of the five Gradle projects (root / core / coe / cews /
#      transmutation) and each of its two resource roots (src/main/resources,
#      src/generated/resources) it walks data/**/*.json and classifies every file by the
#      first path segment under data/<namespace>/ that names a known data carrier
#      (tags, recipe, loot_table, loot_modifier, advancement, enchantment, worldgen,
#      biome_modifier, curios, damage_type), else "other".  Third-party namespace
#      directories (data/c/, data/minecraft/, data/create/, data/curios/, data/neoforge/)
#      are included: they are exactly where tag injections of our ids live.
#   2. REFERENCE EXTRACTION.  Every JSON string VALUE is examined (keys are skipped, so
#      there is no key whitelist and no shape this misses for lack of a known key name).
#      A value is a reference when it matches [ns:path]; "#ns:path" is split out as a tag
#      reference.  Only namespace createoreexpansion is in scope; minecraft: / create: /
#      c: / curios: / neoforge: / anything else is counted as EXTERNAL and never judged.
#   3. INDEX (the hard part).  Two routes, reported separately:
#        route 1a -- ASSET/LOOT PRODUCTS.  A product file whose name is the id:
#                    assets/<ns>/blockstates/<id>.json, assets/<ns>/models/item/<id>.json,
#                    assets/<ns>/models/block/<id>.json, data/<ns>/loot_table/blocks/<id>.json
#                    (the last one also provides the loot-table id <ns>:blocks/<id>).
#                    This is the route that mainly covers blocks and block items.
#        route 1b -- DATA-DEFINITION PRODUCTS.  A data file whose OWN PATH is its registry
#                    id: data/<ns>/{recipe,loot_table,advancement,enchantment,damage_type}/<id>.json,
#                    data/<ns>/worldgen/<kind>/<id>.json, data/<ns>/(neoforge/)biome_modifier/<id>.json,
#                    data/<ns>/loot_modifiers/<id>.json, data/<ns>/tags/<type>/<name>.json
#                    (tag id  #<ns>:<name>).  P7c/P7d bound these products to a layer
#                    explicitly, which is what makes them evidence of ownership.
#        route 2  -- JAVA REGISTRATION CALL LITERALS.  Four documented call-shape rules,
#                    applied to each module's src/main/java after comment stripping:
#                      R1  register("literal")            (DeferredRegister / local helper)
#                      R2  .item|block|blockEntity|entity|fluid|menu("literal")  (Registrate)
#                      R3  LayerRecipeType.processing|serializer("CONSTANT")
#                          -> id = CONSTANT lower-cased (LayerRecipeType#ctor does exactly
#                             Lang.asId(constantName); the id is not the literal as written)
#                      R4  any id-charset literal inside a register(...) ARGUMENT LIST, which
#                          catches the helper shape
#                          AllStructureProcessors.register(event, "bastion_treasure_sapphire",
#                          CODEC) -- the literal is not the first argument, so R1 misses it.
#                          R4 is deliberately the loose rule: it skips the namespace literal
#                          itself and any literal preceded by withDefaultNamespace( (which
#                          declares the minecraft namespace), and it PRINTS every literal it
#                          keeps in section 3, so its residual false positives are inspectable
#                          rather than hidden.
#                    The "literal -> the module that contains the file" mapping is the
#                    ownership claim.  R1/R2/R3 are only sound because every DeferredRegister and
#                    the shared Registrate are built on CoeCore.REGISTRY_NAMESPACE (a project
#                    red line); the script re-checks that mechanically and prints how many
#                    DeferredRegister.create calls use it.
#   4. MODULE -> modId.  Read from <module>/src/main/templates/META-INF/neoforge.mods.toml
#      [[mods]] modId.  PLACEHOLDER POLICY: coe/cews/transmutation write modId = "${mod_id}";
#      that is resolved from <module>/gradle.properties key mod_id.  Nothing is hardcoded:
#      the placeholder name IS the property key.  A template with no [[mods]] block (core,
#      the JarJar shared library) publishes no mod, which is reported, not guessed.
#      Every resolved value is cross-checked against the built jar's own expanded
#      META-INF/neoforge.mods.toml (root jar + META-INF/jarjar/*.jar) when a jar exists.
#   5. REPORTS.  carrier coverage; index coverage per route; the cross-module reference
#      table (referrer module -> target owning module) -- the first full picture of
#      "cross-layer data references" in this tree; UNINDEXED detail with a mechanical
#      sub-classification.
#   6. ANTI-VACUITY GUARDS (the only thing that can fail).  Per carrier category: file floor
#      and in-namespace-reference floor.  Global: data file floor, own-ns reference floor,
#      reference-producing-file floor, index-size floor, route-1/route-2 floors, module
#      floors.  Each failing floor prints the house-format line
#        pattern failure (expected >= N, got A) -- <suspected direction>
#      A pattern-based gate that matches 0 things is worse than no gate (the W6-d
#      "0 matches = silent pass" hole); that is the single hole this tool refuses to repeat.
#      The floors are ANTI-VACUITY FLOORS, not a whitelist: they can only make the tool more
#      red, never less, and they never excuse a specific id or file.  The only hand-written
#      identity table is module -> modId (cross-checked against the jar, see 4).
#   7. -SelfTest.  Builds synthetic trees under build/patch/data-attribution-selftest/ and
#      proves the scanner and the guards really count:
#        S1 a complete synthetic tree must be fully green AND its extraction counts, index
#           counts and cross-module pair must equal the expected values;
#        S2 the same tree with one carrier emptied must be red, naming that carrier;
#        S3 an empty root must make EVERY floor red;
#        S4 a literal JSON text must yield exactly the expected reference list.
#      The synthetic tree is generated FROM the floor table, so the self-test cannot drift
#      away from the floors it is supposed to exercise.
#
# PARSER NOTE
#   Windows PowerShell 5.1 ConvertFrom-Json cannot parse an object whose key is the empty
#   string; that shape exists in this tree's blockstates.  More importantly, this tool does
#   not need a DOM at all -- it needs "every string that is a VALUE, in order".  So it runs
#   the regex  "(?:\\.|[^"\\])*" | [{}\[\]:,]  over the raw text and keeps a two-state
#   machine: a container stack decides whether an object position can hold a key (only right
#   after '{' or ',' inside an object), and the previous significant token decides whether a
#   string IS that key.  Numbers / true / false / null emit no token and only leave the
#   previous token at ',' or ':' -- which is exactly the information the key test needs.
#   Consequence: keys are never reported, and the empty-key file shape is irrelevant.
#   Escapes inside a retained string are left as written (an id never contains one).
#
# BOUNDARY -- what this tool does NOT judge (each is a deliberate, named limit)
#   1. Nothing is FAILED except the anti-vacuity floors.  An unindexed id, a cross-module
#      reference in an illegal direction, a dangling reference -- all are REPORTED.
#   2. Ids BUILT AT RUNTIME are invisible to route 2 by construction.  Known shapes in this
#      tree, all of which route 2 misses and route 1 usually rescues:
#        * helper-method indirection: CoeItems#grindingWheel is called as
#          grindingWheel("iron_grinding_wheel", ...) -- the literal is a plain method
#          argument to a NON-register-named helper, so no rule sees it.  Ten item ids.
#          Route 1a covers them because datagen wrote models/item/<id>.json.
#        * Registrate-derived ids: ".fluid("transmutation_fluid").bucket()" registers
#          "transmutation_fluid_bucket" with no literal anywhere; the fluid also registers
#          "..._flowing" the same way.  Route 1a covers the bucket; the flowing fluid
#          (referenced from a tag) stays UNINDEXED forever.
#        * string concatenation, Lang.asId-style case folding applied to a variable,
#          ids read from a config file or from another registry at runtime.
#      A helper whose literal IS inside a register(...) call is covered by R4; a helper that
#      merely calls a builder is not.  When every route misses, the id lands in UNINDEXED and
#      is CLASSIFIED (see section 5): a suffix of the id that appears as a Java literal is the
#      fingerprint of the first two shapes.  That classification is a heuristic, labelled as
#      one, and it is the raw material for the upgrade criterion U2 in section 9.
#   3. Reference carriers that do not exist in this tree are untested here.  There is no
#      data/<ns>/advancement/ and no data/<ns>/damage_type/ today, so those two carrier
#      categories are reported with 0 files and their floors are 0 by design -- a floor of 1
#      there would be permanently red, i.e. the mistake this phase exists to avoid.  If such
#      a directory appears, add the floor in the same commit.
#   4. Only JSON string values are scanned.  A reference encoded as a JSON NUMBER, as part of
#      a compound key, or inside a non-JSON resource (a .nbt structure, a .snbt, a .mcmeta)
#      is not seen.  NBT structures under data/<ns>/structure/ would be a separate extractor.
#   5. Java-side evidence is comment-stripped by regex, not by a Java lexer: a "//" inside a
#      string literal would truncate the rest of that line, and a "/*" inside a string would
#      swallow code.  Neither occurs in this tree (0 such lines reported below).  Route 2 is
#      built on call SHAPES (names of called methods), never on a list of accepted ids.
#   6. ResourceLocation semantics: an id is lower-cased before lookup and the filesystem is
#      case-insensitive on Windows, so a case-only mismatch is invisible.  0 asset/data
#      filenames in this tree contain an upper-case letter; the tool re-checks that.
#   7. The jar dimension is REPORT ONLY.  build/libs/*.jar is a snapshot: if another process
#      rebuilds it while this runs, the difference is a TIMING fact, not a verdict.  The tool
#      prints each jar's mtime/length and whether it is FRESH or STALE relative to the newest
#      source data file, so a rebuild between two runs is visible rather than confusing.
#   8. The namespace directory is not required to be ours.  A file in data/minecraft/tags/...
#      that references createoreexpansion:foo is in scope (that is how tag injection works);
#      the carrier is classified from the path, the reference from its namespace.
#   9. The index is id-keyed, so TWO providers of one id collapse into a multi-owner set.
#      That is reported (duplicate providers) because it is itself a migration smell -- a
#      "copied instead of moved" file -- but it is not failed here.
#  10. This script writes nothing unless -OutFile is given, and never runs Gradle.
#  11. Id-shaped literals that are NOT registry-entry declarations are excluded from the index
#      on purpose: ResourceLocation.fromNamespaceAndPath("<ns>", "L") and CoeCore.modLoc("L").
#      In this tree those are model / texture / tag / JEI-UID paths. Indexing them would invent
#      owners for ids that are not registry ids, i.e. it would shrink UNINDEXED with FALSE
#      coverage -- worse than a larger UNINDEXED list. Both counts are printed in section 3 so
#      the omission is measurable rather than invisible.
#
# RUN (this machine has no pwsh -- use powershell):
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-data-attribution.ps1
#   optional: -RepoRoot <dir>   scan another checkout
#             -SkipJar          source tree only
#             -OutFile <file>   also write the report (ASCII, LF only)
#             -SelfTest         synthetic-input self-check (writes under build/patch/)
#   exit 0 = every anti-vacuity floor is met (this is the normal, green, observation-only
#            outcome -- unindexed ids and cross-module references do NOT affect it).
#   exit 1 = a floor was not met, i.e. the scanner found (almost) nothing where it must find
#            something.  The full failure list is printed.
#
# ASCII only, LF only.

[CmdletBinding()]
param(
    [string]$RepoRoot = '',
    [switch]$SelfTest,
    [switch]$SkipJar,
    [string]$OutFile = '',
    [int]$TopUnindexed = 40,
    [int]$TopCross = 60
)

$ErrorActionPreference = 'Stop'

if ($RepoRoot -eq '') {
    $RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
}
$RepoRoot = (Resolve-Path -LiteralPath $RepoRoot).Path

$Namespace = 'createoreexpansion'

# ===========================================================================
# 0. constants: module layout, carriers, index routes, floors
# ===========================================================================

$ModuleTable = [ordered]@{
    'root'          = '.'
    'core'          = 'core'
    'coe'           = 'coe'
    'cews'          = 'cews'
    'transmutation' = 'transmutation'
}
$ModuleOrder = @('root', 'core', 'coe', 'cews', 'transmutation')
$ResourceRoots = @('src/main/resources', 'src/generated/resources')
$JavaRootRel = 'src/main/java'
$TomlTemplateRel = 'src/main/templates/META-INF/neoforge.mods.toml'
$TomlJarEntry = 'META-INF/neoforge.mods.toml'

$CarrierOrder = @('tags', 'recipe', 'loot_table', 'loot_modifier', 'advancement',
                  'enchantment', 'worldgen', 'biome_modifier', 'curios',
                  'damage_type', 'other')

# Path segment (anywhere between the namespace dir and the file name) -> carrier.
# Singular and plural spellings are both accepted and canonicalised, because the
# directory name is a data-pack convention, not something this tool may assume.
$CarrierTokens = @{
    'tags'            = 'tags'
    'tag'             = 'tags'
    'recipe'          = 'recipe'
    'recipes'         = 'recipe'
    'loot_table'      = 'loot_table'
    'loot_tables'     = 'loot_table'
    'loot_modifier'   = 'loot_modifier'
    'loot_modifiers'  = 'loot_modifier'
    'advancement'     = 'advancement'
    'advancements'    = 'advancement'
    'enchantment'     = 'enchantment'
    'enchantments'    = 'enchantment'
    'worldgen'        = 'worldgen'
    'biome_modifier'  = 'biome_modifier'
    'biome_modifiers' = 'biome_modifier'
    'curios'          = 'curios'
    'damage_type'     = 'damage_type'
    'damage_types'    = 'damage_type'
}

# Directory (relative to data/<namespace>/) the -SelfTest synthetic tree uses for a carrier.
$CarrierSynthDir = @{
    'tags'           = 'tags/item'
    'recipe'         = 'recipe'
    'loot_table'     = 'loot_table'
    'loot_modifier'  = 'loot_modifiers'
    'advancement'    = 'advancement'
    'enchantment'    = 'enchantment'
    'worldgen'       = 'worldgen/configured_feature'
    'biome_modifier' = 'neoforge/biome_modifier'
    'curios'         = 'curios'
    'damage_type'    = 'damage_type'
    'other'          = 'other'
}

# ---------------------------------------------------------------------------
# ANTI-VACUITY FLOORS.
#
# These are NOT a whitelist.  A whitelist says "this violation is accepted"; a floor says
# "the scanner must still match at least this much, or the scanner is broken".  A floor can
# only ever ADD failures.  Nothing here names an id, a file or a module exception.
#
# Every value is at most half of the count measured in this tree when the tool was written,
# so ordinary churn (moving files between modules, retiring a feature) does not trip it,
# while a dead extractor (0 matches) always does.  Floors are per carrier category plus a
# global block.  A floor of 0 means "this category does not exist in this tree"; it is
# printed as such and deliberately creates no guard, because requiring it would be a
# permanently red gate (see BOUNDARY note 3).
# ---------------------------------------------------------------------------
$script:Floors = @{
    Carriers = [ordered]@{
        'tags'           = @{ Files = 60; Refs = 10 }
        'recipe'         = @{ Files = 160; Refs = 120 }
        'loot_table'     = @{ Files = 35; Refs = 5 }
        'loot_modifier'  = @{ Files = 12; Refs = 12 }
        'advancement'    = @{ Files = 0; Refs = 0 }
        'enchantment'    = @{ Files = 2; Refs = 1 }
        'worldgen'       = @{ Files = 4; Refs = 1 }
        'biome_modifier' = @{ Files = 2; Refs = 2 }
        'curios'         = @{ Files = 1; Refs = 0 }
        'damage_type'    = @{ Files = 0; Refs = 0 }
        'other'          = @{ Files = 0; Refs = 0 }
    }
    # The global floors are deliberately set BELOW the sum of the per-carrier floors.  Reason:
    # the per-carrier floors are the precise instrument (a category that goes empty is named
    # exactly), while the globals only exist to catch "the whole walk is dead".  Keeping the
    # globals small means deleting one carrier category trips THAT category's guards and
    # nothing else -- which is what makes the guards diagnosable instead of a wall of red.
    Global = @{
        DataFiles       = 60
        OwnRefs         = 25
        RefFiles        = 20
        IndexIds        = 60
        Route1aIds      = 40
        Route2Ids       = 40
        ModulesWithData = 3
        ModIdsResolved  = 4
    }
}

# ===========================================================================
# 1. tiny helpers
# ===========================================================================

$script:ReportLines = New-Object System.Collections.Generic.List[string]
$script:ReportPath = ''
if ($OutFile -ne '') {
    # NOTE: the script-scope sink is named ReportPath, NOT OutFile.  At the top level of a
    # script the script scope IS the current scope, so an initialiser like
    #   $script:OutFile = ''
    # OVERWRITES the bound -OutFile PARAMETER with the empty string, and -OutFile then
    # silently writes nothing.  That is exactly what the first version of this tool did.
    $rp = $OutFile
    if (-not [System.IO.Path]::IsPathRooted($rp)) {
        $rp = Join-Path (Get-Location).Path $rp
    }
    $script:ReportPath = $rp
}

function Out-Line {
    param([string]$Text = '')
    Write-Host $Text
    if ($script:ReportPath -ne '') { [void]$script:ReportLines.Add($Text) }
}

function Flush-OutFile {
    if ($script:ReportPath -eq '') { return }
    $dir = Split-Path -Parent $script:ReportPath
    if (($dir -ne '') -and (-not (Test-Path -LiteralPath $dir))) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    Set-Content -LiteralPath $script:ReportPath -Value ($script:ReportLines -join "`n") -NoNewline -Encoding ASCII
}

# The house anti-vacuity line format.
function Get-FloorFailure {
    param([int]$Actual, [int]$Minimum, [string]$Label, [string]$Suspected)
    if ($Minimum -le 0) { return $null }
    if ($Actual -ge $Minimum) { return $null }
    return ('pattern failure (expected >= ' + $Minimum + ', got ' + $Actual + ') -- ' + $Suspected)
}

function Get-FileText {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return '' }
    $t = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
    if ($null -eq $t) { return '' }
    return [string]$t
}

# ===========================================================================
# 2. module -> modId  (template + placeholder policy, never hardcoded)
# ===========================================================================

function Get-TomlModsBlock {
    param([string]$Text)
    $out = New-Object System.Collections.Generic.List[object]
    $cur = $null
    $in = $false
    if ($null -eq $Text) { return @() }
    foreach ($rawLine in ($Text -split "`n")) {
        $line = $rawLine.Replace("`r", '')
        $h = $line.IndexOf('#')
        if ($h -ge 0) { $line = $line.Substring(0, $h) }
        $line = $line.Trim()
        if ($line -eq '') { continue }
        if ($line.StartsWith('[[')) {
            if ($in -and ($null -ne $cur)) { [void]$out.Add($cur) }
            $cur = $null
            $in = $line.StartsWith('[[mods')
            if ($in) { $cur = @{ ModId = '' } }
            continue
        }
        if ($line.StartsWith('[')) {
            if ($in -and ($null -ne $cur)) { [void]$out.Add($cur) }
            $cur = $null
            $in = $false
            continue
        }
        if ((-not $in) -or ($null -eq $cur)) { continue }
        $eq = $line.IndexOf('=')
        if ($eq -lt 0) { continue }
        $k = $line.Substring(0, $eq).Trim()
        $v = $line.Substring($eq + 1).Trim()
        if (($v.Length -ge 2) -and $v.StartsWith('"') -and $v.EndsWith('"')) {
            $v = $v.Substring(1, $v.Length - 2)
        }
        if ($k -ceq 'modId') { $cur.ModId = $v }
    }
    if ($in -and ($null -ne $cur)) { [void]$out.Add($cur) }
    return $out.ToArray()
}

function Get-PropertiesMap {
    param([string]$Path)
    $p = @{}
    if (-not (Test-Path -LiteralPath $Path)) { return $p }
    foreach ($line in (Get-Content -LiteralPath $Path -Encoding UTF8)) {
        $t = [string]$line
        $t = $t.Trim()
        if (($t -eq '') -or $t.StartsWith('#')) { continue }
        $eq = $t.IndexOf('=')
        if ($eq -lt 0) { continue }
        $k = $t.Substring(0, $eq).Trim()
        $v = $t.Substring($eq + 1).Trim()
        if (-not $p.ContainsKey($k)) { $p[$k] = $v }
    }
    return $p
}

function Get-ModuleModIdTable {
    param([string]$Root)
    $res = [ordered]@{}
    foreach ($m in $ModuleOrder) {
        $dir = Join-Path $Root $ModuleTable[$m]
        $e = @{
            Module = $m; Dir = $dir; ModId = ''; Raw = ''; Placeholder = ''
            PropsFile = ''; How = ''; Found = $false
        }
        $props = Get-PropertiesMap (Join-Path $dir 'gradle.properties')
        if (-not (Test-Path -LiteralPath (Join-Path $dir 'gradle.properties'))) {
            $e.PropsFile = '(none)'
        } else {
            $e.PropsFile = 'gradle.properties'
        }
        $tp = Join-Path $dir $TomlTemplateRel
        if (-not (Test-Path -LiteralPath $tp)) {
            $e.How = 'no ' + $TomlTemplateRel + ' -> publishes no mod (JarJar game library)'
            $res[$m] = $e
            continue
        }
        $e.Found = $true
        $mods = @(Get-TomlModsBlock (Get-FileText $tp))
        if ($mods.Count -lt 1) {
            $e.How = 'template has no [[mods]] block -> publishes no mod'
            $res[$m] = $e
            continue
        }
        $raw = [string]$mods[0].ModId
        $e.Raw = $raw
        if ($raw -match '^\$\{([A-Za-z0-9_\.]+)\}$') {
            $key = $Matches[1]
            $e.Placeholder = $key
            if ($props.ContainsKey($key)) {
                $e.ModId = [string]$props[$key]
                $e.How = 'template ${' + $key + '} resolved from gradle.properties ' + $key
            } else {
                $e.How = 'template ${' + $key + '} UNRESOLVED (no such key in gradle.properties)'
            }
        } else {
            $e.ModId = $raw
            $e.How = 'template literal'
        }
        $res[$m] = $e
    }
    return $res
}

# ===========================================================================
# 3. JSON: every string VALUE, in order (see PARSER NOTE in the header)
# ===========================================================================

$script:TokRe = New-Object System.Text.RegularExpressions.Regex ('"(?:\\.|[^"\\])*"|[{}\[\]:,]')
$script:TagRefRe = New-Object System.Text.RegularExpressions.Regex ('^#([A-Za-z0-9_\.\-]+):([A-Za-z0-9_/\.\-]+)$')
$script:IdRefRe = New-Object System.Text.RegularExpressions.Regex ('^([A-Za-z0-9_\.\-]+):([A-Za-z0-9_/\.\-]+)$')

function Get-JsonStringValues {
    param([string]$Text)
    $vals = New-Object System.Collections.Generic.List[string]
    $stack = New-Object System.Collections.Generic.List[char]
    $cOpen = [char]'{'
    $cClose = [char]'}'
    $aOpen = [char]'['
    $aClose = [char]']'
    $cComma = [char]','
    $prev = [char]0
    $malformed = $false
    foreach ($m in $script:TokRe.Matches($Text)) {
        $tok = $m.Value
        if ($tok[0] -eq [char]'"') {
            $isKey = $false
            if ($stack.Count -gt 0) {
                if (($stack[$stack.Count - 1] -eq $cOpen) -and (($prev -eq $cOpen) -or ($prev -eq $cComma))) {
                    $isKey = $true
                }
            }
            if (-not $isKey) {
                if ($tok.Length -ge 2) {
                    [void]$vals.Add($tok.Substring(1, $tok.Length - 2))
                } else {
                    [void]$vals.Add('')
                }
            }
            $prev = [char]'v'
            continue
        }
        $c = $tok[0]
        if (($c -eq $cOpen) -or ($c -eq $aOpen)) {
            $stack.Add($c)
        } elseif (($c -eq $cClose) -or ($c -eq $aClose)) {
            if ($stack.Count -gt 0) { $stack.RemoveAt($stack.Count - 1) } else { $malformed = $true }
        }
        $prev = $c
    }
    if ($stack.Count -ne 0) { $malformed = $true }
    return @{ Values = $vals.ToArray(); Malformed = $malformed }
}

function Get-CanonicalCarrier {
    param([string]$RelData)
    # RelData = path relative to data/  (namespace/.../file.json)
    $parts = $RelData -split '/'
    $i = 1
    while ($i -lt ($parts.Count - 1)) {
        $seg = $parts[$i]
        if ($CarrierTokens.ContainsKey($seg)) { return $CarrierTokens[$seg] }
        $i++
    }
    return 'other'
}

# ===========================================================================
# 4. index evidence: product paths (route 1a / 1b) and java literals (route 2)
# ===========================================================================

function Get-ProductIds {
    param([string]$Rel)   # relative to the module resource root, forward slashes
    $out = New-Object System.Collections.Generic.List[object]
    $ns = $Namespace

    # ---- route 1a: asset / loot products whose NAME is the id -----------------
    if ($Rel -match ('^assets/' + $ns + '/blockstates/([^/]+)\.json$')) {
        [void]$out.Add(@{ Key = ($ns + ':' + $Matches[1]); Kind = 'block'; Route = '1a'; Family = '1a/blockstates' })
        return $out.ToArray()
    }
    if ($Rel -match ('^assets/' + $ns + '/models/item/([^/]+)\.json$')) {
        [void]$out.Add(@{ Key = ($ns + ':' + $Matches[1]); Kind = 'item'; Route = '1a'; Family = '1a/models.item' })
        return $out.ToArray()
    }
    if ($Rel -match ('^assets/' + $ns + '/models/block/([^/]+)\.json$')) {
        [void]$out.Add(@{ Key = ($ns + ':' + $Matches[1]); Kind = 'block'; Route = '1a'; Family = '1a/models.block' })
        return $out.ToArray()
    }

    # ---- route 1a: loot_table/blocks/<id>.json is BOTH a block id and a loot-table id
    if ($Rel -match ('^data/' + $ns + '/loot_table/blocks/([^/]+)\.json$')) {
        $id = $Matches[1]
        [void]$out.Add(@{ Key = ($ns + ':' + $id); Kind = 'block'; Route = '1a'; Family = '1a/loot_table.blocks' })
        [void]$out.Add(@{ Key = ($ns + ':blocks/' + $id); Kind = 'loot_table'; Route = '1b'; Family = '1b/loot_table' })
        return $out.ToArray()
    }

    # ---- route 1b: a data file whose own path is its registry id -------------
    if ($Rel -match ('^data/' + $ns + '/(recipe|advancement|enchantment|damage_type)/(.+)\.json$')) {
        $reg = $Matches[1]
        [void]$out.Add(@{ Key = ($ns + ':' + $Matches[2]); Kind = $reg; Route = '1b'; Family = ('1b/' + $reg) })
        return $out.ToArray()
    }
    if ($Rel -match ('^data/' + $ns + '/loot_table/(.+)\.json$')) {
        [void]$out.Add(@{ Key = ($ns + ':' + $Matches[1]); Kind = 'loot_table'; Route = '1b'; Family = '1b/loot_table' })
        return $out.ToArray()
    }
    if ($Rel -match ('^data/' + $ns + '/loot_modifiers/(.+)\.json$')) {
        [void]$out.Add(@{ Key = ($ns + ':' + $Matches[1]); Kind = 'loot_modifier'; Route = '1b'; Family = '1b/loot_modifiers' })
        return $out.ToArray()
    }
    if ($Rel -match ('^data/' + $ns + '/(neoforge/)?biome_modifier/([^/]+)\.json$')) {
        [void]$out.Add(@{ Key = ($ns + ':' + $Matches[2]); Kind = 'biome_modifier'; Route = '1b'; Family = '1b/biome_modifier' })
        return $out.ToArray()
    }
    if ($Rel -match ('^data/' + $ns + '/worldgen/([^/]+)/(.+)\.json$')) {
        $sub = $Matches[1]
        [void]$out.Add(@{ Key = ($ns + ':' + $Matches[2]); Kind = ('worldgen/' + $sub); Route = '1b'; Family = ('1b/worldgen.' + $sub) })
        return $out.ToArray()
    }
    if ($Rel -match ('^data/' + $ns + '/tags/([^/]+)/(.+)\.json$')) {
        # tag ids drop the registry-type segment: data/<ns>/tags/item/x/y.json -> <ns>:x/y
        [void]$out.Add(@{ Key = ('#' + $ns + ':' + $Matches[2]); Kind = 'tag'; Route = '1b'; Family = '1b/tags' })
        return $out.ToArray()
    }
    return $out.ToArray()
}

# Java: strip comments, then apply three documented call-shape rules.
function Get-JavaStripped {
    param([string]$Text)
    $t = [regex]::Replace($Text, '(?s)/\*.*?\*/', ' ')
    $t = [regex]::Replace($t, '//[^\r\n]*', ' ')
    return $t
}

$script:ReRegisterLit = New-Object System.Text.RegularExpressions.Regex ('\bregister\s*\(\s*"([^"\\\r\n]+)"')
$script:ReFactoryLit = New-Object System.Text.RegularExpressions.Regex ('\.(?:item|block|blockEntity|entity|fluid|menu)\s*\(\s*"([^"\\\r\n]+)"')
$script:ReRecipeTypeLit = New-Object System.Text.RegularExpressions.Regex ('\b(?:processing|serializer)\s*\(\s*"([A-Z0-9_]+)"')
$script:ReAnyJavaLiteral = New-Object System.Text.RegularExpressions.Regex ('"((?:\\.|[^"\\\r\n])*)"')
$script:ReRegisterCallOpen = New-Object System.Text.RegularExpressions.Regex ('\bregister\s*\(')
$script:ReIdCharset = New-Object System.Text.RegularExpressions.Regex ('^[a-z0-9_/\.\-]+$')
$script:ReFromNs = New-Object System.Text.RegularExpressions.Regex ('fromNamespaceAndPath\(\s*"createoreexpansion"\s*,\s*"([^"\\\r\n]+)"')
$script:ReModLoc = New-Object System.Text.RegularExpressions.Regex ('\bmodLoc\s*\(\s*"([^"\\\r\n]+)"')

# Route 2: three call-shape rules.  A rule names a CALL SHAPE, never an accepted id.
#   R1  register("literal")                     -- DeferredRegister, and the local
#                                                  register(name, ...) helpers in this tree
#   R2  .item|block|blockEntity|entity|fluid|menu("literal")  -- Registrate builder factories
#   R3  LayerRecipeType.processing|serializer("CONSTANT")     -- id = CONSTANT lower-cased
#   R4  any id-charset literal inside a register(...) argument list -- catches the helper shape
#       AllStructureProcessors.register(event, "bastion_treasure_sapphire", CODEC), where the
#       literal is NOT the first argument.  The balanced-paren scan is bounded to the call.
function Get-JavaRegLiterals {
    param([string]$Stripped)
    $out = New-Object System.Collections.Generic.List[object]
    foreach ($m in $script:ReRegisterLit.Matches($Stripped)) {
        $lit = $m.Groups[1].Value
        [void]$out.Add(@{ Rule = 'R1 register-literal'; Literal = $lit; Id = $lit })
    }
    foreach ($m in $script:ReFactoryLit.Matches($Stripped)) {
        $lit = $m.Groups[1].Value
        [void]$out.Add(@{ Rule = 'R2 registrate-factory'; Literal = $lit; Id = $lit })
    }
    foreach ($m in $script:ReRecipeTypeLit.Matches($Stripped)) {
        $lit = $m.Groups[1].Value
        [void]$out.Add(@{ Rule = 'R3 recipe-type-constant'; Literal = $lit; Id = $lit.ToLowerInvariant() })
    }
    foreach ($m in $script:ReRegisterCallOpen.Matches($Stripped)) {
        $start = $m.Index + $m.Length
        $depth = 1
        $i = $start
        $end = -1
        while ($i -lt $Stripped.Length) {
            $ch = $Stripped[$i]
            if ($ch -eq '(') { $depth++ }
            elseif ($ch -eq ')') {
                $depth--
                if ($depth -eq 0) { $end = $i; break }
            }
            $i++
        }
        if ($end -lt 0) { continue }
        $arglist = $Stripped.Substring($start, $end - $start)
        foreach ($am in $script:ReAnyJavaLiteral.Matches($arglist)) {
            $lit = $am.Groups[1].Value
            if ($lit.Length -lt 3) { continue }
            if ($lit -ceq $Namespace) { continue }   # the namespace literal itself is not an id
            if (-not $script:ReIdCharset.IsMatch($lit)) { continue }
            # A literal immediately preceded by withDefaultNamespace( is explicitly declared to
            # be in the minecraft namespace (e.g. ItemProperties.register(..., 
            # ResourceLocation.withDefaultNamespace("pull"), ...)), so it cannot be one of our
            # ids.  Without this the loose rule invents "createoreexpansion:pull".
            $prefix = $arglist.Substring(0, $am.Index)
            if ($prefix -match 'withDefaultNamespace\s*\(\s*$') { continue }
            [void]$out.Add(@{ Rule = 'R4 register-arglist-literal'; Literal = $lit; Id = $lit })
        }
    }
    return $out.ToArray()
}

# ===========================================================================
# 5. the scan
# ===========================================================================

function Add-IndexEntry {
    param($Index, [string]$Key, [string]$Route, [string]$Family, [string]$Owner, [string]$Kind)
    if (-not $Index.ContainsKey($Key)) {
        $Index[$Key] = @{
            Routes   = New-Object System.Collections.Generic.HashSet[string]
            Families = New-Object System.Collections.Generic.HashSet[string]
            Owners   = New-Object System.Collections.Generic.HashSet[string]
            Kinds    = New-Object System.Collections.Generic.HashSet[string]
        }
    }
    $e = $Index[$Key]
    [void]$e.Routes.Add($Route)
    [void]$e.Families.Add($Family)
    [void]$e.Owners.Add($Owner)
    [void]$e.Kinds.Add($Kind)
}

function Invoke-Scan {
    param([string]$Root)

    $Floors = $script:Floors
    $R = @{}
    $R.Root = $Root
    $R.ModIds = Get-ModuleModIdTable $Root

    $R.Carriers = [ordered]@{}
    foreach ($c in $CarrierOrder) {
        $R.Carriers[$c] = @{ Files = 0; RefFiles = 0; OwnRefs = 0; TagRefs = 0; IdRefs = 0; ExtRefs = 0; Empty = $false }
    }

    $R.DataFiles = 0
    $R.Malformed = New-Object System.Collections.Generic.List[string]
    $R.UpperCaseNames = New-Object System.Collections.Generic.List[string]
    $R.Refs = New-Object System.Collections.Generic.List[object]
    $R.Index = @{}
    $R.RouteCounts = @{ '1a' = 0; '1b' = 0; '2' = 0 }
    $R.RuleCounts = @{}
    $R.FamilyCounts = @{}
    $R.JavaLiterals = New-Object System.Collections.Generic.HashSet[string]
    $R.Route2WideSamples = New-Object System.Collections.Generic.HashSet[string]
    $R.ExcludedFromNs = 0
    $R.ExcludedModLoc = 0
    $R.DeferredRegisterCalls = 0
    $R.DeferredRegisterOnNamespace = 0

    foreach ($m in $ModuleOrder) {
        $moduleDir = Join-Path $Root $ModuleTable[$m]
        if (-not (Test-Path -LiteralPath $moduleDir)) { continue }

        # ---------------- data files (carrier inventory, refs, route 1b products)
        foreach ($rr in $ResourceRoots) {
            $dataBase = Join-Path $moduleDir ($rr + '/data')
            if (-not (Test-Path -LiteralPath $dataBase)) { continue }
            $files = @(Get-ChildItem -LiteralPath $dataBase -Recurse -File -Filter '*.json' | Sort-Object FullName)
            foreach ($f in $files) {
                $R.DataFiles++
                $relData = ($f.FullName.Substring($dataBase.Length).TrimStart('\', '/')).Replace('\', '/')
                $carrier = Get-CanonicalCarrier $relData
                if ($f.Name -cmatch '[A-Z]') { [void]$R.UpperCaseNames.Add($f.FullName) }
                $R.Carriers[$carrier].Files++

                # route 1b (and 1a for loot_table/blocks) evidence from the path itself
                foreach ($prec in @(Get-ProductIds ('data/' + $relData))) {
                    Add-IndexEntry $R.Index $prec.Key $prec.Route $prec.Family $m $prec.Kind
                }

                $parsed = Get-JsonStringValues (Get-FileText $f.FullName)
                if ($parsed.Malformed) { [void]$R.Malformed.Add(('data/' + $relData)) }
                $fileRefs = 0
                foreach ($v in $parsed.Values) {
                    $kind = ''
                    $ns = ''
                    $path = ''
                    $mt = $script:TagRefRe.Match($v)
                    if ($mt.Success) {
                        $kind = 'tag'; $ns = $mt.Groups[1].Value; $path = $mt.Groups[2].Value
                    } else {
                        $mi = $script:IdRefRe.Match($v)
                        if ($mi.Success) {
                            $kind = 'id'; $ns = $mi.Groups[1].Value; $path = $mi.Groups[2].Value
                        }
                    }
                    if ($kind -eq '') { continue }
                    if ($ns -cne $Namespace) { $R.Carriers[$carrier].ExtRefs++; continue }
                    $key = $ns + ':' + $path
                    if ($kind -eq 'tag') {
                        $key = '#' + $key
                        $R.Carriers[$carrier].TagRefs++
                    } else {
                        $R.Carriers[$carrier].IdRefs++
                    }
                    $R.Carriers[$carrier].OwnRefs++
                    $fileRefs++
                    [void]$R.Refs.Add(@{
                        Module = $m; Rel = ('data/' + $relData); Carrier = $carrier
                        Kind = $kind; Key = $key; Ns = $ns; Path = $path
                    })
                }
                if ($fileRefs -gt 0) { $R.Carriers[$carrier].RefFiles++ }
            }
        }

        # ---------------- route 1a asset products
        foreach ($rr in $ResourceRoots) {
            $assetNs = Join-Path $moduleDir ($rr + '/assets/' + $Namespace)
            if (-not (Test-Path -LiteralPath $assetNs)) { continue }
            foreach ($sub in @('blockstates', 'models/item', 'models/block')) {
                $d = Join-Path $assetNs ($sub.Replace('/', '\'))
                if (-not (Test-Path -LiteralPath $d)) { continue }
                foreach ($f in @(Get-ChildItem -LiteralPath $d -File -Filter '*.json' | Sort-Object FullName)) {
                    if ($f.Name -cmatch '[A-Z]') { [void]$R.UpperCaseNames.Add($f.FullName) }
                    $rel = 'assets/' + $Namespace + '/' + $sub + '/' + $f.Name
                    foreach ($prec in @(Get-ProductIds $rel)) {
                        Add-IndexEntry $R.Index $prec.Key $prec.Route $prec.Family $m $prec.Kind
                    }
                }
            }
        }

        # ---------------- route 2 java registration literals
        $javaBase = Join-Path $moduleDir $JavaRootRel
        if (Test-Path -LiteralPath $javaBase) {
            foreach ($jf in @(Get-ChildItem -LiteralPath $javaBase -Recurse -File -Filter '*.java' | Sort-Object FullName)) {
                $raw = Get-FileText $jf.FullName
                if ($raw -eq '') { continue }
                $R.DeferredRegisterCalls += [regex]::Matches($raw, 'DeferredRegister\.create').Count
                $R.DeferredRegisterOnNamespace += [regex]::Matches($raw, 'DeferredRegister\.create[A-Za-z]*\([^;]*CoeCore\.REGISTRY_NAMESPACE').Count
                $R.ExcludedFromNs += $script:ReFromNs.Matches($raw).Count
                $R.ExcludedModLoc += $script:ReModLoc.Matches($raw).Count
                $stripped = Get-JavaStripped $raw
                foreach ($lm in $script:ReAnyJavaLiteral.Matches($stripped)) {
                    [void]$R.JavaLiterals.Add($lm.Groups[1].Value)
                }
                foreach ($lit in @(Get-JavaRegLiterals $stripped)) {
                    Add-IndexEntry $R.Index ($Namespace + ':' + $lit.Id) '2' ('2/' + $lit.Rule) $m 'registration-literal'
                    if ($lit.Rule -ceq 'R4 register-arglist-literal') {
                        [void]$R.Route2WideSamples.Add($lit.Literal)
                    }
                }
            }
        }
    }

    # ---------------- index roll-ups (DISTINCT ids per route / family) ------------
    $R.RouteCounts = @{ '1a' = 0; '1b' = 0; '2' = 0 }
    $R.FamilyCounts = @{}
    $R.RuleCounts = @{}
    foreach ($k in $R.Index.Keys) {
        foreach ($rt in $R.Index[$k].Routes) { $R.RouteCounts[$rt]++ }
        foreach ($fm in $R.Index[$k].Families) {
            if (-not $R.FamilyCounts.ContainsKey($fm)) { $R.FamilyCounts[$fm] = 0 }
            $R.FamilyCounts[$fm]++
        }
    }
    foreach ($fm in $R.FamilyCounts.Keys) {
        if ($fm.StartsWith('2/')) { $R.RuleCounts[$fm.Substring(2)] = $R.FamilyCounts[$fm] }
    }

    # ---------------- resolution -------------------------------------------------
    $R.CrossTable = @{}            # 'referrer|target' -> @{Refs; Ids=HashSet}
    $R.ResolvedIds = New-Object System.Collections.Generic.HashSet[string]
    $R.UnindexedIds = New-Object System.Collections.Generic.HashSet[string]
    $R.UnindexedOcc = @{}
    $R.ResolvedOcc = 0
    $R.ModuleDataFiles = @{}
    $R.ModuleOwnRefs = @{}

    foreach ($ref in $R.Refs) {
        if (-not $R.ModuleOwnRefs.ContainsKey($ref.Module)) { $R.ModuleOwnRefs[$ref.Module] = 0 }
        $R.ModuleOwnRefs[$ref.Module]++
        if (-not $R.ModuleDataFiles.ContainsKey($ref.Module)) { $R.ModuleDataFiles[$ref.Module] = 0 }
        $owners = @()
        if ($R.Index.ContainsKey($ref.Key)) { $owners = $R.Index[$ref.Key].Owners }
        if ($owners.Count -eq 0) {
            [void]$R.UnindexedIds.Add($ref.Key)
            if (-not $R.UnindexedOcc.ContainsKey($ref.Key)) { $R.UnindexedOcc[$ref.Key] = 0 }
            $R.UnindexedOcc[$ref.Key]++
            $owners = @('UNINDEXED')
        } else {
            [void]$R.ResolvedIds.Add($ref.Key)
            $R.ResolvedOcc++
        }
        foreach ($o in $owners) {
            $tk = $ref.Module + '|' + $o
            if (-not $R.CrossTable.ContainsKey($tk)) {
                $R.CrossTable[$tk] = @{ Referrer = $ref.Module; Target = $o; Refs = 0; Ids = New-Object System.Collections.Generic.HashSet[string] }
            }
            $R.CrossTable[$tk].Refs++
            [void]$R.CrossTable[$tk].Ids.Add($ref.Key)
        }
    }

    # per-module data file counts (whole module, not only files with refs)
    foreach ($m in $ModuleOrder) {
        $n = 0
        foreach ($rr in $ResourceRoots) {
            $d = Join-Path (Join-Path $Root $ModuleTable[$m]) ($rr + '/data')
            if (Test-Path -LiteralPath $d) {
                $n += @(Get-ChildItem -LiteralPath $d -Recurse -File -Filter '*.json').Count
            }
        }
        $R.ModuleDataFiles[$m] = $n
    }

    # ---------------- duplicate providers (report only) ---------------------------
    $R.DuplicateProviders = New-Object System.Collections.Generic.List[object]
    foreach ($k in $R.Index.Keys) {
        $owners = $R.Index[$k].Owners
        if ($owners.Count -gt 1) {
            [void]$R.DuplicateProviders.Add(@{ Key = $k; Owners = $owners })
        }
    }

    # ---------------- UNINDEXED classification (heuristic, labelled) ---------------
    $R.UnindexedClass = @{}
    foreach ($k in $R.UnindexedIds) {
        $cls = 'no-literal-anywhere(maybe dangling, maybe runtime-built)'
        if ($k.StartsWith('#')) {
            $cls = 'tag-not-provided'
        } else {
            $path = $k.Substring($k.IndexOf(':') + 1)
            if ($R.JavaLiterals.Contains($path)) {
                $cls = 'literal-in-java-but-not-in-a-registration-call'
            } else {
                $i = 0
                while (($i = $path.IndexOf('_', $i)) -ge 0) {
                    $suf = $path.Substring($i + 1)
                    $suf2 = $path.Substring($i)
                    if (($suf -ne '') -and $R.JavaLiterals.Contains($suf)) { $cls = 'fragment-literal(suffix after _)'; break }
                    if ($R.JavaLiterals.Contains($suf2)) { $cls = 'fragment-literal(leading _)'; break }
                    $i++
                }
            }
        }
        $R.UnindexedClass[$k] = $cls
    }

    # ---------------- guards ------------------------------------------------------
    $R.GuardTotal = 0
    $R.GuardFailures = New-Object System.Collections.Generic.List[object]

    $check = {
        param([int]$Actual, [int]$Min, [string]$Label, [string]$Suspected)
        if ($Min -le 0) { return }
        $R.GuardTotal++
        $msg = Get-FloorFailure $Actual $Min $Label $Suspected
        if ($null -ne $msg) { [void]$R.GuardFailures.Add(@{ Name = $Label; Text = $msg }) }
    }

    foreach ($c in $CarrierOrder) {
        $f = $Floors.Carriers[$c]
        $st = $R.Carriers[$c]
        if ($f.Files -gt 0) {
            & $check $st.Files $f.Files ('carrier/' + $c + '/files') ('the ' + $c + ' carrier vanished from data/**: either the directory moved (classifying it as "other"), the scanner path logic broke, or the data was deleted')
        }
        if ($f.Refs -gt 0) {
            & $check $st.OwnRefs $f.Refs ('carrier/' + $c + '/ownRefs') ('files exist but no ' + $Namespace + ': reference was extracted from them')
        }
    }
    & $check $R.DataFiles $Floors.Global.DataFiles 'global/dataFiles' 'the data/**/*.json walk found almost nothing: check the five module directories and the two resource roots'
    & $check $R.Refs.Count $Floors.Global.OwnRefs 'global/ownRefs' 'almost no own-namespace reference was extracted: check the JSON value tokenizer and the ns:path regex'
    # NOTE: never wrap a generic List in @() -- in Windows PowerShell 5.1 that raises
    # "Argument types do not match", and @(<List> | Group-Object ...) silently collapses to
    # one group.  Assign the pipeline first, then read .Count.
    $refGroups = $R.Refs | Group-Object Module, Rel
    & $check $refGroups.Count $Floors.Global.RefFiles 'global/refProducingFiles' 'refs were found but no file reports any: the per-file ref counter broke'
    & $check $R.Index.Count $Floors.Global.IndexIds 'global/indexIds' 'the id index is nearly empty: check both product routes and the java literal routes'
    & $check $R.RouteCounts['1a'] $Floors.Global.Route1aIds 'global/route1aIds' 'route 1a (blockstates / models / loot_table.bl) found almost nothing: check the asset product walk'
    & $check $R.RouteCounts['2'] $Floors.Global.Route2Ids 'global/route2Ids' 'route 2 (java registration literals) found almost nothing: check the java walk and the three call-shape rules'
    & $check (Get-ModulesWithData $R) $Floors.Global.ModulesWithData 'global/modulesWithData' 'fewer modules carry data/ than expected: a resource root or a module directory is not being walked'
    & $check (Get-ModIdsResolved $R) $Floors.Global.ModIdsResolved 'global/modIdsResolved' 'fewer module -> modId resolutions than expected: check the mods.toml templates and the gradle.properties placeholder fallback'

    return $R
}

function Get-ModulesWithData {
    param($R)
    $n = 0
    foreach ($k in $R.ModuleDataFiles.Keys) { if ($R.ModuleDataFiles[$k] -gt 0) { $n++ } }
    return $n
}

function Get-ModIdsResolved {
    param($R)
    $n = 0
    foreach ($k in $R.ModIds.Keys) { if ($R.ModIds[$k].ModId -ne '') { $n++ } }
    return $n
}

# ===========================================================================
# 6. jar cross-check (report only)
# ===========================================================================

function Get-JarFacts {
    param([string]$Root)
    $out = @{ Found = $false; Path = ''; Length = 0; Mtime = $null; ModIds = @(); Nested = @() }
    $libs = Join-Path $Root 'build/libs'
    if (-not (Test-Path -LiteralPath $libs)) { return $out }
    $jars = @(Get-ChildItem -LiteralPath $libs -File -Filter '*.jar' | Sort-Object Name)
    if ($jars.Count -eq 0) { return $out }
    $j = $jars[0]
    $out.Found = $true
    $out.Path = $j.FullName
    $out.Length = $j.Length
    $out.Mtime = $j.LastWriteTime
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
        $zip = [System.IO.Compression.ZipFile]::OpenRead($j.FullName)
        $entry = $zip.Entries | Where-Object { $_.FullName -eq $TomlJarEntry } | Select-Object -First 1
        if ($null -ne $entry) {
            $sr = New-Object System.IO.StreamReader($entry.Open(), [System.Text.Encoding]::UTF8)
            try { $txt = $sr.ReadToEnd() } finally { $sr.Dispose() }
            foreach ($mb in @(Get-TomlModsBlock $txt)) {
                if ($mb.ModId -ne '') { $out.ModIds += $mb.ModId }
            }
        }
        foreach ($e in $zip.Entries) {
            if ($e.FullName.EndsWith('/')) { continue }
            if (-not $e.FullName.StartsWith('META-INF/jarjar/')) { continue }
            if (-not $e.FullName.EndsWith('.jar')) { continue }
            $rec = @{ Entry = $e.FullName; ModIds = @(); Read = $false }
            try {
                $ms = New-Object System.IO.MemoryStream
                $cs = $e.Open()
                $cs.CopyTo($ms)
                $cs.Dispose()
                [void]$ms.Seek(0, [System.IO.SeekOrigin]::Begin)
                $nz = New-Object System.IO.Compression.ZipArchive($ms, [System.IO.Compression.ZipArchiveMode]::Read)
                $ne = $nz.Entries | Where-Object { $_.FullName -eq $TomlJarEntry } | Select-Object -First 1
                if ($null -ne $ne) {
                    $sr2 = New-Object System.IO.StreamReader($ne.Open(), [System.Text.Encoding]::UTF8)
                    try { $t2 = $sr2.ReadToEnd() } finally { $sr2.Dispose() }
                    foreach ($mb2 in @(Get-TomlModsBlock $t2)) {
                        if ($mb2.ModId -ne '') { $rec.ModIds += $mb2.ModId }
                    }
                    $rec.Read = $true
                }
                $nz.Dispose()
                $ms.Dispose()
            } catch {
                $rec.Read = $false
            }
            $out.Nested += $rec
        }
        $zip.Dispose()
    } catch {
        $out.Found = $false
    }
    return $out
}

function Get-NewestDataMtime {
    param([string]$Root)
    $newest = $null
    foreach ($m in $ModuleOrder) {
        foreach ($rr in $ResourceRoots) {
            $d = Join-Path (Join-Path $Root $ModuleTable[$m]) ($rr + '/data')
            if (-not (Test-Path -LiteralPath $d)) { continue }
            foreach ($f in @(Get-ChildItem -LiteralPath $d -Recurse -File -Filter '*.json')) {
                if (($null -eq $newest) -or ($f.LastWriteTime -gt $newest)) { $newest = $f.LastWriteTime }
            }
        }
    }
    return $newest
}

# ===========================================================================
# 7. report
# ===========================================================================

function Show-Report {
    param($R, [bool]$IsSelfTest, [string]$Label)

    Out-Line ''
    Out-Line '================================================================================'
    Out-Line ' DATA-ATTRIBUTION -- cross-module data reference attribution (OBSERVATION ONLY)'
    Out-Line '================================================================================'
    Out-Line (' repo            : ' + $R.Root)
    Out-Line (' namespace       : ' + $Namespace + '  (only this namespace is in scope)')
    Out-Line (' run label       : ' + $Label)
    Out-Line ' exit semantics  : exit 1 ONLY from the anti-vacuity floors in section 6.'
    Out-Line '                   UNINDEXED ids and cross-module references never fail.'
    Out-Line ''

    Out-Line '--- SECTION 1/9 :: MODULE -> modId (template + placeholder resolution) -----------'
    Out-Line ('  {0,-15} {1,-20} {2}' -f 'module', 'modId', 'how')
    foreach ($m in $ModuleOrder) {
        $e = $R.ModIds[$m]
        $mid = $e.ModId
        if ($mid -eq '') { $mid = '(none)' }
        Out-Line ('  {0,-15} {1,-20} {2}' -f $m, $mid, $e.How)
    }
    Out-Line ('  DeferredRegister.create calls: ' + $R.DeferredRegisterCalls + ' total, ' + $R.DeferredRegisterOnNamespace + ' on CoeCore.REGISTRY_NAMESPACE')
    if ($R.DeferredRegisterCalls -ne $R.DeferredRegisterOnNamespace) {
        Out-Line ('  NOTE: ' + ($R.DeferredRegisterCalls - $R.DeferredRegisterOnNamespace) + ' DeferredRegister.create(...) call(s) do NOT use REGISTRY_NAMESPACE -> route 2 (R1) would map those literals to the wrong namespace. REPORT ONLY.')
    }
    Out-Line ''

    Out-Line '--- SECTION 2/9 :: CARRIER COVERAGE --------------------------------------------'
    Out-Line ('  {0,-16} {1,6} {2,9} {3,8} {4,8} {5,7} {6,8} {7,7}' -f 'carrier', 'files', 'refFiles', 'ownRefs', 'tagRefs', 'idRefs', 'extRefs', 'floor')
    foreach ($c in $CarrierOrder) {
        $st = $R.Carriers[$c]
        $f = $script:Floors.Carriers[$c]
        Out-Line ('  {0,-16} {1,6} {2,9} {3,8} {4,8} {5,7} {6,8} {7,7}' -f $c, $st.Files, $st.RefFiles, $st.OwnRefs, $st.TagRefs, $st.IdRefs, $st.ExtRefs, ($f.Files.ToString() + '/' + $f.Refs.ToString()))
    }
    Out-Line ('  total data files: ' + $R.DataFiles + '   json files that failed the tokenizer balance check: ' + $R.Malformed.Count)
    if ($R.Malformed.Count -gt 0) {
        foreach ($x in @($R.Malformed | Select-Object -First 10)) { Out-Line ('    MALFORMED: ' + $x) }
    }
    Out-Line ('  data file names containing an upper-case letter (ResourceLocation case assumption): ' + $R.UpperCaseNames.Count)
    Out-Line ('  per module data file counts: ' + (($R.ModuleDataFiles.Keys | Sort-Object | ForEach-Object { $_ + '=' + $R.ModuleDataFiles[$_] }) -join '  '))
    Out-Line ''

    Out-Line '--- SECTION 3/9 :: ID INDEX COVERAGE (distinct ids per route) -----------------'
    Out-Line ('  route 1a (asset / loot products)           : ' + $R.RouteCounts['1a'] + ' ids')
    Out-Line ('  route 1b (data-definition products)        : ' + $R.RouteCounts['1b'] + ' ids')
    Out-Line ('  route 2  (java registration literals)      : ' + $R.RouteCounts['2'] + ' ids')
    Out-Line ('  UNION index size (distinct ids)            : ' + $R.Index.Count)
    $both = 0; $only1 = 0; $only2 = 0; $tagKeys = 0
    foreach ($k in $R.Index.Keys) {
        $rs = @($R.Index[$k].Routes | ForEach-Object { $_ })
        if ($k.StartsWith('#')) { $tagKeys++ }
        if ($rs -contains '2') {
            if (($rs -contains '1a') -or ($rs -contains '1b')) { $both++ } else { $only2++ }
        } else {
            $only1++
        }
    }
    Out-Line ('    covered by both routes 1 and 2           : ' + $both)
    Out-Line ('    covered by route 1 only                  : ' + $only1)
    Out-Line ('    covered by route 2 only                  : ' + $only2)
    Out-Line ('    tag-form keys in the index (#ns:name)    : ' + $tagKeys)
    Out-Line '  families:'
    foreach ($fam in ($R.FamilyCounts.Keys | Sort-Object)) {
        Out-Line ('    ' + $fam.PadRight(34) + ' ' + $R.FamilyCounts[$fam])
    }
    Out-Line '  route 2 rules (distinct ids whose evidence came from that rule):'
    foreach ($rule in ($R.RuleCounts.Keys | Sort-Object)) {
        Out-Line ('    ' + $rule.PadRight(34) + ' ' + $R.RuleCounts[$rule])
    }
    if ($R.RouteCounts['2'] -eq 0) {
        Out-Line '    WARNING: route 2 matched no literal at all.'
    }
    if ($R.Route2WideSamples.Count -gt 0) {
        Out-Line ('  R4 (the loose rule) captured these literals -- inspect them for false positives:')
        foreach ($s in @($R.Route2WideSamples | Sort-Object)) { Out-Line ('    ' + $s) }
    }
    Out-Line '  id-shaped literals deliberately NOT used as index evidence (see SECTION 8 note 8):'
    Out-Line ('    ResourceLocation.fromNamespaceAndPath("' + $Namespace + '", "L") : ' + $R.ExcludedFromNs + ' occurrences')
    Out-Line ('    CoeCore.modLoc("L")                                          : ' + $R.ExcludedModLoc + ' occurrences')
    Out-Line '    These are mostly model/texture/tag paths, not registry entries; indexing them would'
    Out-Line '    invent owners for ids that are not registry ids. Counted here so the omission is visible.'
    if ($R.DuplicateProviders.Count -gt 0) {
        Out-Line ('  duplicate providers (same id from >1 module) : ' + $R.DuplicateProviders.Count + '  <- "copied instead of moved" smell, or a legitimate shared id')
        foreach ($d in @($R.DuplicateProviders | Sort-Object { $_.Key } | Select-Object -First 20)) {
            Out-Line ('    ' + $d.Key + '  <= ' + ($d.Owners -join ', '))
        }
    } else {
        Out-Line '  duplicate providers (same id from >1 module) : 0'
    }
    Out-Line ''

    Out-Line '--- SECTION 4/9 :: CROSS-MODULE REFERENCE TABLE (referrer -> owner) ------------'
    Out-Line ('  {0,-15} {1,-15} {2,7} {3,7} {4}' -f 'referrer', 'target', 'refs', 'ids', 'note')
    $rows = @($R.CrossTable.Values | Sort-Object -Property @{ Expression = { $_.Referrer } }, @{ Expression = { $_.Target } })
    $shown = 0
    foreach ($row in $rows) {
        if ($shown -ge $TopCross) {
            Out-Line ('  ... ' + ($rows.Count - $TopCross) + ' more (referrer,target) pairs suppressed by -TopCross')
            break
        }
        $note = ''
        if ($row.Target -eq 'UNINDEXED') { $note = 'no owner found by either route' }
        elseif ($row.Target -eq $row.Referrer) { $note = 'same module' }
        else { $note = 'CROSS-MODULE' }
        Out-Line ('  {0,-15} {1,-15} {2,7} {3,7} {4}' -f $row.Referrer, $row.Target, $row.Refs, $row.Ids.Count, $note)
        $shown++
    }
    $crossPairs = @($rows | Where-Object { ($_.Target -ne $_.Referrer) -and ($_.Target -ne 'UNINDEXED') })
    $crossRefs = 0
    foreach ($x in $crossPairs) { $crossRefs += $x.Refs }
    Out-Line ('  cross-module (referrer != owner) pairs: ' + $crossPairs.Count + '   reference occurrences: ' + $crossRefs)
    Out-Line ''

    Out-Line '--- SECTION 5/9 :: UNINDEXED DETAIL --------------------------------------------'
    $unOcc = 0
    foreach ($k in $R.UnindexedIds) { $unOcc += $R.UnindexedOcc[$k] }
    Out-Line ('  distinct ids no route could locate : ' + $R.UnindexedIds.Count)
    Out-Line ('  reference occurrences of those ids : ' + $unOcc)
    Out-Line ('  distinct ids that DID resolve      : ' + $R.ResolvedIds.Count + '   occurrences: ' + $R.ResolvedOcc)
    $byClass = @{}
    foreach ($k in $R.UnindexedIds) {
        $c = $R.UnindexedClass[$k]
        if (-not $byClass.ContainsKey($c)) { $byClass[$c] = 0 }
        $byClass[$c]++
    }
    Out-Line '  classification (heuristic -- see BOUNDARY note 2):'
    foreach ($c in ($byClass.Keys | Sort-Object)) {
        Out-Line ('    ' + $c.PadRight(52) + ' ' + $byClass[$c])
    }
    Out-Line ('  first ' + $TopUnindexed + ' distinct UNINDEXED ids (by occurrence count):')
    $sorted = @($R.UnindexedIds | Sort-Object -Property @{ Expression = { - $R.UnindexedOcc[$_] } }, @{ Expression = { $_ } })
    $n = 0
    foreach ($k in $sorted) {
        if ($n -ge $TopUnindexed) { break }
        Out-Line ('    ' + $R.UnindexedOcc[$k].ToString().PadLeft(4) + ' x  ' + $k.PadRight(56) + ' [' + $R.UnindexedClass[$k] + ']')
        $n++
    }
    if ($sorted.Count -gt $TopUnindexed) {
        Out-Line ('    ... ' + ($sorted.Count - $TopUnindexed) + ' more (raise -TopUnindexed to see them)')
    }
    Out-Line ''

    Out-Line '--- SECTION 6/9 :: ANTI-VACUITY GUARDS (the ONLY failure source) --------------'
    Out-Line ('  guards evaluated: ' + $R.GuardTotal + '   failures: ' + $R.GuardFailures.Count)
    if ($R.GuardFailures.Count -eq 0) {
        Out-Line '  PASS  every floor is met.'
    } else {
        foreach ($g in $R.GuardFailures) {
            Out-Line ('  FAIL  ' + $g.Name)
            Out-Line ('        ' + $g.Text)
        }
    }
    Out-Line ''

    if (-not $SkipJar) {
        Out-Line '--- SECTION 7/9 :: JAR CROSS-CHECK (report only, never fails) ------------------'
        $jf = Get-JarFacts $R.Root
        if (-not $jf.Found) {
            Out-Line '  no build/libs/*.jar found (or it could not be opened) -- template values stand alone.'
        } else {
            Out-Line ('  jar            : ' + $jf.Path)
            Out-Line ('  length / mtime : ' + $jf.Length + ' B / ' + $jf.Mtime.ToString('yyyy-MM-dd HH:mm:ss'))
            $newest = Get-NewestDataMtime $R.Root
            if ($null -ne $newest) {
                $verdict = 'FRESH'
                if ($jf.Mtime -lt $newest) { $verdict = 'STALE (source data is newer than the jar)' }
                Out-Line ('  newest source data mtime : ' + $newest.ToString('yyyy-MM-dd HH:mm:ss') + '  -> jar verdict: ' + $verdict)
                Out-Line '  A jar is a snapshot. If it changes between two runs of this tool, that is a concurrent'
                Out-Line '  rebuild (another job), i.e. a TIMING fact -- not a finding of this tool.'
            }
            Out-Line ('  [[mods]] in the root jar   : ' + (($jf.ModIds | Sort-Object) -join ', '))
            foreach ($nd in $jf.Nested) {
                $mid = '(no mods.toml)'
                if ($nd.ModIds.Count -gt 0) { $mid = ($nd.ModIds -join ', ') }
                Out-Line ('  nested ' + ($nd.Entry -replace '^META-INF/jarjar/', '') + ' -> ' + $mid)
            }
            $jarIds = New-Object System.Collections.Generic.HashSet[string]
            foreach ($x in $jf.ModIds) { [void]$jarIds.Add($x) }
            foreach ($nd in $jf.Nested) { foreach ($x in $nd.ModIds) { [void]$jarIds.Add($x) } }
            $tplIds = New-Object System.Collections.Generic.HashSet[string]
            foreach ($m in $ModuleOrder) { if ($R.ModIds[$m].ModId -ne '') { [void]$tplIds.Add($R.ModIds[$m].ModId) } }
            $onlyJar = @($jarIds | Where-Object { -not $tplIds.Contains($_) })
            $onlyTpl = @($tplIds | Where-Object { -not $jarIds.Contains($_) })
            Out-Line ('  template-only modIds : ' + $(if ($onlyTpl.Count -eq 0) { '(none)' } else { ($onlyTpl -join ', ') }))
            Out-Line ('  jar-only modIds      : ' + $(if ($onlyJar.Count -eq 0) { '(none)' } else { ($onlyJar -join ', ') }))
            Out-Line '    jar-only entries are expected for third-party JarJar payloads (e.g. skiller); anything else'
            Out-Line '    means the template table above and the shipped artefact disagree.'
        }
        Out-Line ''
    }

    Out-Line '--- SECTION 8/9 :: BOUNDARY -- what this scan CANNOT see -----------------------'
    Out-Line '  1. Runtime-built ids. Route 2 sees literals only, through four call-shape rules'
    Out-Line '     (R1 register-first-arg, R2 Registrate factory, R3 recipe-type constant, R4 any'
    Out-Line '     id-charset literal inside a register(...) argument list). Known shapes in this tree:'
    Out-Line '       * helper-method indirection, e.g. CoeItems calls grindingWheel("iron_grinding_wheel",'
    Out-Line '         ...): the literal is an argument to a non-register-named helper -> no rule sees it;'
    Out-Line '         route 1a rescues it (datagen wrote models/item/iron_grinding_wheel.json).'
    Out-Line '       * Registrate-derived ids, e.g. ".fluid("transmutation_fluid").bucket()" registers'
    Out-Line '         "transmutation_fluid_bucket"; the fluid registers "..._flowing" the same way.'
    Out-Line '         Route 1a rescues the bucket (models/item/transmutation_fluid_bucket.json); the'
    Out-Line '         flowing fluid is UNINDEXED forever.'
    Out-Line '       * a reference to an id that was DELIBERATELY de-registered but is still named by a'
    Out-Line '         tag entry with "required": false (findable here: UNINDEXED with class'
    Out-Line '         "no-literal-anywhere" -- see machines_light in this tree). The game ignores it'
    Out-Line '         silently; so does every build gate; this report is the only place it shows up.'
    Out-Line '       * string concatenation / case folding of a variable / ids read from config or from'
    Out-Line '         another registry at runtime -> every route can miss it; it lands in UNINDEXED.'
    Out-Line '     HOW IT FAILS: silently, as an UNINDEXED row -- never as a false FAIL (by design of this'
    Out-Line '     phase). A future assertion phase must therefore stay evidence-based, not id-based.'
    Out-Line '  2. Reference carriers absent from this tree are untested: no data/<ns>/advancement/ and no'
    Out-Line '     data/<ns>/damage_type/ exist today, so their floors are 0 (see BOUNDARY note 3 in the'
    Out-Line '     header). If such a directory is added, add the floor in the same commit or the new'
    Out-Line '     carrier is scanned but unguarded.'
    Out-Line '  3. Only JSON string VALUES. A reference encoded as a number, inside a compound key, or in a'
    Out-Line '     non-JSON resource (.nbt structure, .snbt, .mcmeta) is invisible. data/<ns>/structure/'
    Out-Line '     would need its own NBT extractor.'
    Out-Line '  4. Comment stripping is regex-based, not a Java lexer: a "//" inside a Java string literal'
    Out-Line '     truncates the rest of that line; "/*" inside a string would swallow code. Neither occurs'
    Out-Line '     in this tree today.'
    Out-Line '  5. Route 1b treats "the data file exists" as "the registry entry exists". A datagen output'
    Out-Line '     that is never loaded (wrong directory spelling -- the tags/item vs tags/items trap in'
    Out-Line '     AGENTS.md) still produces an index entry here. This tool does NOT validate data-pack'
    Out-Line '     path spelling; runData does not either for hand-written files.'
    Out-Line '  6. Route 2 assumes the registration literal names the id. For R3 the id is derived by'
    Out-Line '     lower-casing; for Registrate the id is the literal as written. A registration that'
    Out-Line '     transforms its argument another way is mis-indexed, silently, as a wrong owner.'
    Out-Line '  7. Cross-module direction is NOT judged here. That is intentional: the allowed-direction'
    Out-Line '     predicate needs the required-dependency closure, which this phase reports but does not'
    Out-Line '     apply. See section 9.'
    Out-Line '  8. Id-shaped literals that are NOT registry-entry declarations are deliberately excluded'
    Out-Line '     from the index: ResourceLocation.fromNamespaceAndPath("<ns>", "L") and CoeCore.modLoc("L")'
    Out-Line '     (counted in section 3). In this tree they are model / texture / tag / JEI-UID paths,'
    Out-Line '     and indexing them would invent owners for ids that are not registry ids -- i.e. it'
    Out-Line '     would shrink UNINDEXED with false coverage, which is worse than a larger UNINDEXED list.'
    Out-Line ''

    Out-Line '--- SECTION 9/9 :: UPGRADE PATH (documented, NOT implemented) -------------------'
    Out-Line '  This is an observation tool. It becomes an attribution ASSERTION only when all of the'
    Out-Line '  following hold, measured over consecutive runs on leaf-dev:'
    Out-Line '    U1  UNINDEXED distinct ids == 0 for N consecutive runs (suggest N >= 3), AND'
    Out-Line '    U2  every id that was ever UNINDEXED is either (a) now indexed by route 1a/1b/2, or'
    Out-Line '        (b) explicitly recorded in the header as a runtime-built id with a manual'
    Out-Line '        confirmation and the mechanism that builds it (the two known shapes are the'
    Out-Line '        grindingWheel helper and the Registrate fluid bucket), AND'
    Out-Line '    U3  the duplicate-provider list is empty, or every entry is explained (an id with two'
    Out-Line '        providers makes owner(X) ambiguous, so the assertion below cannot be evaluated), AND'
    Out-Line '    U4  route 2 covers the registries that carry the references -- measured as: every'
    Out-Line '        registry referenced from data/** (recipe types, enchantments, features, biomes,'
    Out-Line '        loot modifiers, damage types) has at least one route-1b or route-2 index entry.'
    Out-Line ''
    Out-Line '  The assertion that may then be enabled (one added guard, still nothing else):'
    Out-Line '    for every in-scope reference written in module M to id X:'
    Out-Line '        owner(X)  must be a subset of  M  UNION  required-closure(M)'
    Out-Line '    where required-closure(M) is built exactly the way check-asset-attribution.ps1 builds'
    Out-Line '    it: read [[dependencies.*]] blocks with type = "required" from every module template,'
    Out-Line '    keep the ones whose modId is one of the modules ownIds, and take the transitive closure.'
    Out-Line '    This is the direction judgement that must NOT be skipped: cews and transmutation declare'
    Out-Line '    createoreexpansion required, so "cews -> coe" is legal; coe declares no local required'
    Out-Line '    dependency, so "coe -> cews" is not. A gate without the direction predicate is'
    Out-Line '    permanently red on a legal tree, and a permanently red gate gets learned-ignored.'
    Out-Line '    Reporting form for the failure: referrer module + file + carrier + id + the module that'
    Out-Line '    actually owns it + the closure that was consulted.'
    Out-Line '  Until U1..U4 hold, keep this tool green-by-construction: report, never fail.'
    Out-Line ''
}

# ===========================================================================
# 8. synthetic-input self-test
# ===========================================================================

function New-SyntheticTree {
    param([string]$TreeDir, [string]$HideCarrier)

    if (Test-Path -LiteralPath $TreeDir) { Remove-Item -LiteralPath $TreeDir -Recurse -Force }
    New-Item -ItemType Directory -Path $TreeDir -Force | Out-Null

    $tomlRoot = @'
modLoader = "javafml"
loaderVersion = "[1,)"
license = "test"
[[mods]]
modId = "coe_integration"
version = "1.0.0"
'@
    $tomlMod = @'
modLoader = "javafml"
loaderVersion = "[1,)"
license = "test"
[[mods]]
modId = "${mod_id}"
version = "1.0.0"
'@

    Set-Content -LiteralPath (Join-Path $TreeDir 'gradle.properties') -Value "mod_id=coe_integration`n" -NoNewline -Encoding ASCII
    $tplRoot = Join-Path $TreeDir 'src/main/templates/META-INF'
    New-Item -ItemType Directory -Path $tplRoot -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $tplRoot 'neoforge.mods.toml') -Value $tomlRoot -NoNewline -Encoding ASCII

    $modIds = @{ 'coe' = 'createoreexpansion'; 'cews' = 'cews'; 'transmutation' = 'transmutation' }
    foreach ($m in $modIds.Keys) {
        $md = Join-Path $TreeDir $m
        New-Item -ItemType Directory -Path $md -Force | Out-Null
        Set-Content -LiteralPath (Join-Path $md 'gradle.properties') -Value ('mod_id=' + $modIds[$m] + "`n") -NoNewline -Encoding ASCII
        $t = Join-Path $md 'src/main/templates/META-INF'
        New-Item -ItemType Directory -Path $t -Force | Out-Null
        Set-Content -LiteralPath (Join-Path $t 'neoforge.mods.toml') -Value $tomlMod -NoNewline -Encoding ASCII
    }

    # Write-Synth takes its root EXPLICITLY and refuses a non-rooted root.  This is not
    # decoration: the first version relied on the enclosing function's $Dir, and a local
    # $dir (PowerShell variable names are case-insensitive) silently rebound it to the
    # carrier directory -- after which every synthetic data file was written relative to the
    # CURRENT DIRECTORY, i.e. into the real repository root (tags/, recipe/, ...).  The
    # rooted-root check turns that whole class of accident into an immediate, loud error.
    function Write-Synth {
        param([string]$RootDir, [string]$Rel, [string]$Content)
        if (-not [System.IO.Path]::IsPathRooted($RootDir)) {
            throw ('synthetic tree root must be an absolute path, got: ' + $RootDir)
        }
        if ([System.IO.Path]::IsPathRooted($Rel)) {
            throw ('synthetic relative path must not be rooted: ' + $Rel)
        }
        $p = Join-Path $RootDir $Rel.Replace('/', '\')
        $pd = Split-Path -Parent $p
        if (-not (Test-Path -LiteralPath $pd)) { New-Item -ItemType Directory -Path $pd -Force | Out-Null }
        Set-Content -LiteralPath $p -Value $Content -NoNewline -Encoding ASCII
    }

    # ---- route 1a products (spread over two modules so cross-module is exercisable)
    $r1a = [int]$script:Floors.Global.Route1aIds
    for ($i = 1; $i -le $r1a; $i++) {
        if (($i % 2) -eq 0) {
            Write-Synth $TreeDir ('coe/src/generated/resources/assets/' + $Namespace + '/models/item/synth_item_' + $i + '.json') ('{"parent":"minecraft:item/generated"}' + "`n")
        } else {
            Write-Synth $TreeDir ('cews/src/generated/resources/assets/' + $Namespace + '/blockstates/synth_block_' + $i + '.json') ('{"variants":{"":{"model":"' + $Namespace + ':block/synth_block_' + $i + '"}}}' + "`n")
        }
    }

    # ---- route 2 java literals
    $r2 = [int]$script:Floors.Global.Route2Ids
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.Append("package synth;`npublic final class SynthReg {`n")
    for ($i = 1; $i -le $r2; $i++) {
        [void]$sb.Append('  public static final Object R' + $i + ' = REG.register("synth_reg_' + $i + '", null);' + "`n")
    }
    [void]$sb.Append('  /* a comment that must not be counted: REG.register("synth_commented_out", null); */' + "`n")
    [void]$sb.Append('  // REG.register("synth_line_commented_out", null);' + "`n")
    [void]$sb.Append('  public static final Object T = LayerRecipeType.processing("SYNTH_RECIPE_TYPE", null);' + "`n")
    [void]$sb.Append("}`n")
    Write-Synth $TreeDir 'coe/src/main/java/synth/SynthReg.java' ($sb.ToString())
    Write-Synth $TreeDir 'cews/src/main/java/synth/SynthReg2.java' ("package synth;`npublic final class SynthReg2 {`n  static { X.register(`"synth_cews_reg`", null); }`n}`n")

    # ---- carrier data files, generated FROM the floors (so this self-test cannot drift away
    #      from the floors it is supposed to exercise).  Files are round-robined over three
    #      modules so that the ModulesWithData floor and the cross-module table are both real.

    $roundRobin = @('coe', 'cews', 'transmutation')
    foreach ($c in $CarrierOrder) {
        if ($c -eq $HideCarrier) { continue }
        $fl = $script:Floors.Carriers[$c]
        $nf = [int]$fl.Files
        if ($nf -le 0) { continue }
        $nr = [int]$fl.Refs

        $carrierDir = $CarrierSynthDir[$c]
        for ($i = 1; $i -le $nf; $i++) {
            $vals = New-Object System.Collections.Generic.List[string]
            if ($nr -gt 0) {
                $j = $i
                while ($j -le $nr) {
                    if ($c -eq 'tags') {
                        # file k is the product for tag id #createoreexpansion:synth_tags_<k>
                        if (($j % 2) -eq 0) { [void]$vals.Add('#createoreexpansion:synth_tags_' + $j) }
                        else { [void]$vals.Add('createoreexpansion:synth_tags_' + $j) }
                    } else {
                        [void]$vals.Add('createoreexpansion:synth_' + $c + '_ref_' + $j)
                    }
                    $j += $nf
                }
            }
            [void]$vals.Add('minecraft:stone')
            $body = '{' + "`n" + '  "values": [' + "`n"
            $k = 0
            foreach ($v in $vals) {
                $k++
                $body += '    "' + $v + '"'
                if ($k -lt $vals.Count) { $body += ',' }
                $body += "`n"
            }
            $body += '  ]' + "`n" + '}' + "`n"
            $owner = $roundRobin[($i - 1) % $roundRobin.Count]
            Write-Synth $TreeDir ($owner + '/src/generated/resources/data/' + $Namespace + '/' + $carrierDir + '/synth_' + $c + '_' + $i + '.json') $body
        }
    }

    # ---- one deliberate cross-module reference: cews writes a recipe that names an id owned by coe
    if ($HideCarrier -ne 'recipe') {
        Write-Synth $TreeDir ('cews/src/generated/resources/data/' + $Namespace + '/recipe/synth_cross.json') ('{"type":"' + $Namespace + ':synth_recipe_type","x":"' + $Namespace + ':synth_item_2"}' + "`n")
    }
    # ---- one module-local reference: coe writes data files naming ids coe itself provides
    #      (for enchantment the product's own path IS the id: .../enchantment/synth_local.json,
    #       and for recipe file 1: .../recipe/synth_recipe_ref_1.json)
    Write-Synth $TreeDir ('coe/src/generated/resources/data/' + $Namespace + '/enchantment/synth_local.json') ('{"v":"' + $Namespace + ':synth_local"}' + "`n")
    Write-Synth $TreeDir ('coe/src/generated/resources/data/' + $Namespace + '/recipe/synth_recipe_ref_1.json') ('{"v":"' + $Namespace + ':synth_recipe_ref_1"}' + "`n")
}

function New-EmptyTree {
    param([string]$Dir)
    if (Test-Path -LiteralPath $Dir) { Remove-Item -LiteralPath $Dir -Recurse -Force }
    New-Item -ItemType Directory -Path $Dir -Force | Out-Null
}

function Invoke-SelfTest {
    param([string]$Root)

    $base = Join-Path $Root 'build/patch/data-attribution-selftest'
    $script:SelfTestFailures = New-Object System.Collections.Generic.List[string]
    $script:SelfTestChecks = 0

    function Assert-True {
        param([bool]$Ok, [string]$Name, [string]$Detail)
        $script:SelfTestChecks++
        if ($Ok) {
            Out-Line ('  PASS  ' + $Name)
        } else {
            Out-Line ('  FAIL  ' + $Name)
            [void]$script:SelfTestFailures.Add($Name)
        }
        if ($Detail -ne '') { Out-Line ('        ' + $Detail) }
    }

    Out-Line ''
    Out-Line '================================================================================'
    Out-Line ' SELF-TEST -- synthetic input, proving the scanner and the guards really count'
    Out-Line '================================================================================'
    Out-Line (' scratch root: ' + $base)
    Out-Line ''

    # ---------------- S4: pure extractor unit test on literal text -----------------
    Out-Line '--- S4 :: extractor unit test on literal JSON text ----------------------------'
    $unit = '{"a":"createoreexpansion:one","b":["createoreexpansion:two","#createoreexpansion:taggy","minecraft:stone",42,"x:y"],"c":{"d":"createoreexpansion:three"},"e":"noseparator","f":"createoreexpansion:deep/path/x"}'
    $pv = Get-JsonStringValues $unit
    $expect = @('createoreexpansion:one', 'createoreexpansion:two', '#createoreexpansion:taggy', 'minecraft:stone', 'x:y', 'createoreexpansion:three', 'noseparator', 'createoreexpansion:deep/path/x')
    $got = @($pv.Values)
    Assert-True (($got.Count -eq $expect.Count) -and (($got -join '|') -ceq ($expect -join '|'))) 'S4.1 values-only extraction, keys skipped' ('expected ' + $expect.Count + ' values, got ' + $got.Count + ': ' + ($got -join ' | '))
    $own = 0; $tag = 0; $ext = 0
    foreach ($v in $got) {
        $mt = $script:TagRefRe.Match($v)
        if ($mt.Success) {
            if ($mt.Groups[1].Value -ceq $Namespace) { $tag++ } else { $ext++ }
            continue
        }
        $mi = $script:IdRefRe.Match($v)
        if ($mi.Success) {
            if ($mi.Groups[1].Value -ceq $Namespace) { $own++ } else { $ext++ }
        }
    }
    Assert-True (($own -eq 4) -and ($tag -eq 1) -and ($ext -eq 2)) 'S4.2 shape split (id / tag / external)' ('own=' + $own + ' tag=' + $tag + ' external=' + $ext + ' (expected 4/1/2)')
    $bad = Get-JsonStringValues '{"a":"createoreexpansion:x"'
    Assert-True ([bool]$bad.Malformed) 'S4.3 unbalanced JSON is flagged malformed' ('malformed=' + $bad.Malformed)
    $keyish = Get-JsonStringValues '{"createoreexpansion:keylookalike":"v"}'
    Assert-True ((@($keyish.Values | ForEach-Object { $_ }).Count -eq 1) -and (@($keyish.Values | ForEach-Object { $_ })[0] -ceq 'v')) 'S4.4 a key that looks like an id is still a key, not a reference' ('values=' + (@($keyish.Values) -join ' | '))
    Assert-True ((Get-CanonicalCarrier 'createoreexpansion/neoforge/biome_modifier/x.json') -ceq 'biome_modifier') 'S4.5 carrier: nested biome_modifier' ''
    Assert-True ((Get-CanonicalCarrier 'c/tags/item/ingots/jade.json') -ceq 'tags') 'S4.6 carrier: third-party namespace tag dir' ''
    Assert-True ((Get-CanonicalCarrier 'createoreexpansion/loot_modifiers/chests/x.json') -ceq 'loot_modifier') 'S4.7 carrier: loot_modifiers plural' ''
    $prod = @(Get-ProductIds 'data/createoreexpansion/loot_table/blocks/jade_ore.json')
    Assert-True (($prod.Count -eq 2) -and (@($prod | ForEach-Object { $_.Key }) -contains 'createoreexpansion:jade_ore') -and (@($prod | ForEach-Object { $_.Key }) -contains 'createoreexpansion:blocks/jade_ore')) 'S4.8 loot_table/blocks yields BOTH the block id and the loot-table id' (($prod | ForEach-Object { $_.Key }) -join ' | ')
    $tp = @(Get-ProductIds 'data/createoreexpansion/tags/item/grinding_wheels/tier_1.json')
    Assert-True (($tp.Count -eq 1) -and ($tp[0].Key -ceq '#createoreexpansion:grinding_wheels/tier_1')) 'S4.9 tag product drops the registry-type segment' (($tp | ForEach-Object { $_.Key }) -join ' | ')
    $jl = @(Get-JavaRegLiterals (Get-JavaStripped ('a.register("one", x); b.block("two", y); LayerRecipeType.processing("THREE_FOUR", z); /* register("commented", q); */ // register("linecommented", q);')))
    $jlit = @($jl | ForEach-Object { $_.Id } | Sort-Object -Unique)
    $jexp = @('one', 'three_four', 'two')
    Assert-True (($jlit.Count -eq 3) -and (($jlit -join '|') -ceq ($jexp -join '|'))) 'S4.10 java rules R1/R2/R3/R4 + comment stripping (distinct ids)' ('literals=' + ($jlit -join ' | '))
    $jr4 = @($jl | Where-Object { $_.Rule -ceq 'R4 register-arglist-literal' } | ForEach-Object { $_.Literal } | Sort-Object -Unique)
    Assert-True (($jr4.Count -eq 1) -and ($jr4[0] -ceq 'one')) 'S4.11 R4 sees only id-charset literals inside register(...) arg lists' ('R4 captured: ' + ($jr4 -join ' | '))
    $jr4b = @(Get-JavaRegLiterals ('helper.register(event, "bastion_treasure_sapphire", CODEC); helper.register(event, "end_ship_stellarstone", CODEC);') | Where-Object { $_.Rule -ceq 'R4 register-arglist-literal' } | ForEach-Object { $_.Literal } | Sort-Object)
    Assert-True (($jr4b.Count -eq 2) -and ($jr4b[0] -ceq 'bastion_treasure_sapphire')) 'S4.12 the helper shape (literal is not the first argument) is covered by R4' ('R4 captured: ' + ($jr4b -join ' | '))

    # ---------------- S1: complete synthetic tree -> fully green --------------------
    Out-Line ''
    Out-Line '--- S1 :: complete synthetic tree -> every floor must be met (GREEN) -----------'
    $d1 = Join-Path $base 's1-green'
    New-SyntheticTree $d1 ''
    $R1 = Invoke-Scan $d1
    Show-Report $R1 $true 'S1 synthetic complete'
    Assert-True ($R1.GuardFailures.Count -eq 0) 'S1.1 guards all pass on the complete synthetic tree' ('failures: ' + $R1.GuardFailures.Count + ' / ' + $R1.GuardTotal + ' guards; ' + (($R1.GuardFailures | ForEach-Object { $_.Name }) -join ', '))
    Assert-True ($R1.GuardTotal -ge 10) 'S1.2 the guard block is not vacuous itself' ('guards evaluated: ' + $R1.GuardTotal)
    Assert-True ($R1.Refs.Count -ge $script:Floors.Global.OwnRefs) 'S1.3 own-namespace refs were extracted' ('refs: ' + $R1.Refs.Count)
    $x1 = @($R1.CrossTable.Values | Where-Object { ($_.Referrer -ceq 'cews') -and ($_.Target -ceq 'coe') })
    Assert-True ($x1.Count -eq 1) 'S1.4 the deliberate cews -> coe reference is attributed to coe' ('matches: ' + $x1.Count)
    $x2 = @($R1.CrossTable.Values | Where-Object { ($_.Referrer -ceq 'coe') -and ($_.Target -ceq 'coe') })
    Assert-True ($x2.Count -eq 1) 'S1.5 a module-local reference resolves to its own module' ('matches: ' + $x2.Count)
    Assert-True (@($R1.UnindexedIds) -notcontains '#createoreexpansion:synth_tags_2') 'S1.7 a "#tag" reference resolves against a route-1b tag product' ('tag keys in index: ' + @($R1.Index.Keys | Where-Object { $_.StartsWith('#') }).Count)
    Assert-True ($R1.ModIds['coe'].ModId -ceq $Namespace) 'S1.6 ${mod_id} placeholder resolved from gradle.properties' ('coe modId = ' + $R1.ModIds['coe'].ModId)

    # ---------------- S2: one carrier emptied -> that carrier must go red ----------
    Out-Line ''
    Out-Line '--- S2 :: hide one carrier category ("recipe") -> that carrier must FAIL -------'
    $d2 = Join-Path $base 's2-hidden-recipe'
    New-SyntheticTree $d2 'recipe'
    $R2 = Invoke-Scan $d2
    Show-Report $R2 $true 'S2 recipe carrier hidden'
    $hit = @($R2.GuardFailures | Where-Object { $_.Name -ceq 'carrier/recipe/files' })
    Assert-True ($hit.Count -eq 1) 'S2.1 the hidden carrier reports pattern failure' ($(if ($hit.Count -eq 1) { $hit[0].Text } else { 'failures: ' + (($R2.GuardFailures | ForEach-Object { $_.Name }) -join ', ') }))
    $unrelated = @($R2.GuardFailures | Where-Object { ($_.Name -cne 'carrier/recipe/files') -and ($_.Name -cne 'carrier/recipe/ownRefs') })
    Assert-True ($unrelated.Count -eq 0) 'S2.2 nothing else goes red (the hidden carrier is the only finding)' ('other failures: ' + (($unrelated | ForEach-Object { $_.Name }) -join ', '))
    Assert-True (($R2.GuardFailures.Count -lt $R2.GuardTotal) -and ($R2.GuardFailures.Count -gt 0)) 'S2.3 partial red, not all-red' ('failures ' + $R2.GuardFailures.Count + ' of ' + $R2.GuardTotal + ' guards')

    # ---------------- S3: empty root -> everything red -----------------------------
    Out-Line ''
    Out-Line '--- S3 :: empty root -> EVERY floor must FAIL ---------------------------------'
    $d3 = Join-Path $base 's3-empty'
    New-EmptyTree $d3
    $R3 = Invoke-Scan $d3
    Show-Report $R3 $true 'S3 empty root'
    Assert-True ($R3.GuardTotal -gt 0) 'S3.1 guards were evaluated at all' ('guards: ' + $R3.GuardTotal)
    Assert-True ($R3.GuardFailures.Count -eq $R3.GuardTotal) 'S3.2 every single floor failed' ('failures ' + $R3.GuardFailures.Count + ' of ' + $R3.GuardTotal + ' guards')
    Assert-True ($R3.DataFiles -eq 0) 'S3.3 zero data files found' ('dataFiles: ' + $R3.DataFiles)

    Out-Line ''
    Out-Line '--- SELF-TEST SUMMARY ---------------------------------------------------------'
    Out-Line ('  checks: ' + $script:SelfTestChecks + '   failures: ' + $script:SelfTestFailures.Count)
    if ($script:SelfTestFailures.Count -gt 0) {
        foreach ($f in $script:SelfTestFailures) { Out-Line ('  FAIL  ' + $f) }
    } else {
        Out-Line '  PASS  all synthetic-input self-checks behaved exactly as specified.'
    }
    return $script:SelfTestFailures.Count
}

# ===========================================================================
# 9. entry point
# ===========================================================================

if ($SelfTest) {
    $selfFails = Invoke-SelfTest $RepoRoot
    Flush-OutFile
    if ($selfFails -gt 0) { exit 1 }
    exit 0
}

$scan = Invoke-Scan $RepoRoot
Show-Report $scan $false 'working tree'
Flush-OutFile

Out-Line ''
Out-Line (' EXIT=' + $(if ($scan.GuardFailures.Count -eq 0) { '0' } else { '1' }) + '  (guards: ' + $scan.GuardTotal + ' evaluated, ' + $scan.GuardFailures.Count + ' failed)')
Out-Line ''

if ($scan.GuardFailures.Count -gt 0) { exit 1 }
exit 0
