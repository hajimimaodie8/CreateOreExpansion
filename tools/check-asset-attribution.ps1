# check-asset-attribution.ps1 -- asset attribution gate (static, read-only, exit 0/1).
#
# WHY THIS EXISTS
#   W6-c moved the three stress chargers' JAVA into :coe but left their hand-written
#   models and textures (71 files: 24 under models/block/{jade,sapphire,stellarstone}_
#   stress_charger/, 47 under textures/block/... same three directory names) inside
#   cews/src/main/resources.  :coe/src/main/resources assets/.../textures/block/ has no
#   *_stress_charger directory at all.  So "install coe.jar only" places the machine but
#   renders it with missing models/textures -- a player-visible defect.
#
#   NOTHING static could see it:
#     * the 88 module-self-sufficiency assertions cover mixin / JEI / lang / shared data /
#       recipe counts / cross-layer hand-written tags -- no assertion covers ASSET OWNERSHIP;
#     * check-layering and layer-usage only look at *.java;
#     * check-package-overlap only looks at Java packages;
#     * compileJava does not read resources;
#     * runData runs with all four mod files present, so every reference resolves;
#     * the published-form A1 test runs a SERVER, which never loads models/textures.
#   This script is that missing gate.
#
# WHAT IT CHECKS (every assertion prints PASS/FAIL together with the actual numbers)
#   A0 anti-vacuity guards.  A pattern-based gate that matches 0 things is worse than no
#      gate (the W6-d "0 matches = silent pass" hole); so every reference category and
#      every namespace bucket must be non-empty, and every asset JSON must parse.
#   A1 ATTRIBUTION + DEPENDENCY DIRECTION (the point of this script): for each module
#      (root / core / coe / cews / transmutation) and for each of its resource roots
#      (<module>/src/generated/resources and <module>/src/main/resources), every reference
#      to an id in OUR OWN namespace (createoreexpansion) that appears in that module's own
#      asset JSON must resolve to a file inside the SAME module's resource roots -- OR
#      inside a module that this module MECHANICALLY DECLARES as required.
#        Why the direction matters: cews and transmutation declare
#          [[dependencies."<their modid>"]] modId = "createoreexpansion" type = "required"
#        so a player installing cews.jar always has coe.jar as well, and "cews -> coe" is a
#        legal asset reference.  coe declares only third-party required deps, so "coe ->
#        cews" is illegal -- coe can be installed alone and the asset would be missing.
#        The dependency set is READ, never guessed and never whitelisted:
#          * [[dependencies.<owner>]] blocks in <module>/src/main/templates/META-INF/
#            neoforge.mods.toml with type = "required" give the required modIds;
#          * a modId is local when it is one of the modules' own modIds (one documented
#            identity table below, cross-checked against the built jar when one exists);
#          * the allowed set is the TRANSITIVE closure, so a future "cews requires X
#            requires coe" chain is handled without touching this script.
#        A gate that ignored the direction would be permanently red on a perfectly legal
#        tree, and a permanently red gate gets learned-ignored (the check-package-heritage
#        lesson in AGENTS.md).  So: illegal direction = FAIL, legal direction = reported.
#        anchors      : assets/createoreexpansion/blockstates/*.json  (blocks)
#                       assets/createoreexpansion/models/item/*.json (items)
#        reference map:
#          blockstate  variants.*.model / variants.*[].model        -> model id
#                      multipart[].apply.model / apply[].model      -> model id
#          model       parent                                       -> model id
#                      overrides[].model                            -> model id
#                      textures.*                                   -> texture id
#        id -> file  : model   createoreexpansion:block/foo/bar -> assets/.../models/block/foo/bar.json
#                      texture createoreexpansion:block/foo     -> assets/.../textures/block/foo.png
#      Only OUR namespace is judged.  minecraft: / create: / any third-party namespace is an
#      external dependency and is counted, not judged.  A namespace-less id defaults to
#      minecraft: (ResourceLocation semantics) and is therefore also out of scope.
#      Every illegal reference is reported with: referencing file (module + relative path),
#      reference kind (parent / model / texture), target id, AND the module that actually
#      holds the bytes -- "lives in: cews" is the evidence of a stranded asset.
#      An id that NO module provides is DANGLING and always fails: direction cannot excuse
#      a reference to something that does not exist anywhere.
#   A2 duplicate provider.  No in-namespace asset id may be provided twice: neither by two
#      different modules (the "copy instead of move" half-migration shape, which leaves two
#      divergent copies in two jars and silently makes A1 green) nor by the two resource
#      roots of one module (which this script's id-keyed index would silently collapse, and
#      which processResources reports as a duplicate).
#   A3 texture-variable resolvability (bounded, see BOUNDARY).  A "#name" texture reference
#      must be defined by the model itself or by an ancestor model reachable entirely
#      through models this script can read.  If the parent chain leaves the readable set
#      (an unresolvable or third-party parent) the site is exempt and counted.
#   A4 cross-module asset inventory (report, also drives A1's failure list): from each
#      module's anchors the script walks the reference graph ACROSS modules and splits what
#      it needs from elsewhere into LEGAL (licensed by a required dependency) and ILLEGAL
#      (no such declaration) -- grouped by owner module, kind and owner directory, with
#      orphan counts.  The illegal half is the "how much is left to move" table.
#   A5 parser self-check.  asset JSON is parsed by a hand-rolled reader (see PARSER NOTE);
#      for every file the platform's own ConvertFrom-Json can also read, both parsers must
#      produce the identical reference list.  A file count of 0 here is a failure.
#   A6 jar dimension (report only, never fails).  The same scan is run over the built
#      build/libs/*.jar entries, because the SHIPPED artefact is what a player installs.
#      Each jar is labelled FRESH or STALE BUILD by comparing the jar's mtime with the
#      newest asset mtime of its module: a jar is a snapshot, and a difference caused by a
#      concurrent edit is a TIMING fact, not this script's verdict.  Jar dependency
#      direction is read from the jar's own META-INF/neoforge.mods.toml.
#
# PARSER NOTE (this is not gratuitous)
#   Windows PowerShell 5.1 ConvertFrom-Json cannot parse a JSON object whose key is the
#   empty string ("Cannot process argument because the value of argument name is not
#   valid").  25 of the 57 blockstate files in this tree are exactly that shape -- datagen
#   emits  "variants": { "": { "model": ... } }  for blocks without properties.  A gate
#   built on ConvertFrom-Json would silently skip 44% of the block anchors, i.e. it would
#   reproduce the very hole it exists to close.  Hence the hand-rolled reader; A5 proves it
#   agrees with ConvertFrom-Json on every file ConvertFrom-Json can read.
#
# BOUNDARY -- what this script does NOT judge (each is a deliberate, named limit)
#   1. Third-party namespaces (minecraft:, create:, ...) are never judged: an external
#      texture that does not exist cannot be seen from here.  Counted, not judged.
#   2. Namespace-less ids are treated as minecraft: (that is what ResourceLocation.parse
#      does), so a bare "block/foo" is NOT read as ours.  In this tree all 111 bare parents
#      are minecraft:block/* (verified), so nothing of ours hides in that bucket.
#   3. Only assets/** is judged.  data/** (recipes, tags, loot tables, worldgen, ...) and
#      lang/** are out of scope; cross-layer data references are a separate gate (see the
#      feasibility assessment in build/patch/w10-EVIDENCE.txt).
#   4. Assets are only judged through the reference shapes listed under A1.  Reference
#      carriers that do not exist in this tree are therefore untested here: particles/*.json,
#      atlases/*.json, font/*.json (they would need their own extractor).  A count of 0
#      "loader" models (neoforge:obj + top-level "model": "...obj") is printed so the
#      omission stays visible; an obj-loader model file would be reported as uncovered
#      rather than silently ignored.
#   5. Reachability is what anchors make visible.  A model/texture that no anchor chain
#      reaches (an "orphan sidecar") is still scanned for A1/A2 if it is an asset JSON, but
#      it is NOT forced by A1's inventory; A4 prints orphan counts per owner directory so a
#      migration can move the whole directory instead of only the referenced files.
#   6. Texture-variable chains that leave the readable model set are exempt from A3.
#   7. A model's own "elements[].faces[].texture" is a texture VARIABLE ("#0"), never a
#      resource id, so it is covered by A3 only; it can never be a cross-module leak.
#   8. Model/texture ids are lower-cased before lookup (ResourceLocation semantics) and the
#      filesystem is case-insensitive on Windows, so a case-only filename mismatch is
#      invisible here.  0 asset filenames in this tree contain an upper-case letter; A0
#      re-checks that every run, so the assumption cannot rot silently.
#   9. The jar scan treats each jar as its own module and does not expand
#      META-INF/jarjar/*.jar; the nested jars are byte-copies of the module jars, and the
#      aggregate root jar owns no assets of its own.
#  10. Dependency direction is read from neoforge.mods.toml ONLY.  A resource that a module
#      reaches through Java at run time (a ResourceLocation built in code, a renderer, a
#      particle registered in code) is invisible to every assertion here.
#  11. The module -> modId identity table below is the one hand-maintained fact in this
#      script.  It is not a whitelist of accepted violations: it only says which mod id each
#      Gradle project publishes, and A0 refuses to run the direction logic when a built jar
#      contradicts it.  Nothing else is hand-maintained: required dependencies, the allowed
#      set and every reference are read mechanically.
#
# RUN (this machine has no pwsh -- use powershell):
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-asset-attribution.ps1
#   optional:  -RepoRoot <dir>    scan another checkout
#              -SkipJars          source tree only
#   exit 0 = every asset reference of every module resolves inside that module or inside a
#            module it declares as required.
#   exit 1 = illegal-direction or dangling reference(s); the full list is printed.
#
# PERTURBATION (how this gate was proven live; raw output in build/patch/w10-EVIDENCE.txt.
# Every perturbation is applied to a throwaway `git archive <rev>` copy under build/patch/,
# so the shared working tree is never touched -- the command sequence is identical in-tree.)
#   P0  pristine 847d10ff (the revision named in the task) -> red, exactly 71 stranded files
#       (24 charger models + 47 charger textures), all coe -> cews (coe requires nothing local)
#   P1  move those 71 charger assets cews -> coe in that copy -> green
#   P2  move ONE needed charger texture coe -> cews            -> red, naming that reference
#   P3  move it back                                          -> green
#   P4  make the scanner match nothing (empty repo root)       -> A0 red ("got 0")
#   P4b hide one category (rename blockstates/)                -> A0 red for that category
#   P5  LEGAL direction: move a texture a cews model needs from cews -> coe
#       -> STILL GREEN (cews declares createoreexpansion required); reported under A4
#   P6  ILLEGAL direction: move a texture a coe model needs coe -> cews
#       -> RED (coe declares no local required dependency)
#
# ASCII only, LF only, read-only (it writes nothing).

[CmdletBinding()]
param(
    [string]$RepoRoot = '',
    [switch]$SkipJars
)

$ErrorActionPreference = 'Stop'

if ($RepoRoot -eq '') {
    $RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
}
$RepoRoot = (Resolve-Path -LiteralPath $RepoRoot).Path

$Namespace = 'createoreexpansion'
$AssetBase = 'assets/' + $Namespace + '/'

$script:checkCount = 0
$script:failures = New-Object System.Collections.Generic.List[string]

function Write-Check {
    param([bool]$Ok, [string]$Name, [string]$Detail)
    $script:checkCount++
    if ($Ok) {
        Write-Host ('PASS  ' + $Name)
    } else {
        Write-Host ('FAIL  ' + $Name)
        $script:failures.Add($Name)
    }
    if ($Detail -ne '') { Write-Host ('        ' + $Detail) }
}

function Write-Sub {
    param([string]$Text)
    Write-Host ('        ' + $Text)
}

# The anti-vacuity guard used by every "at least N matches" assertion in this tree.
# Returns $null when the pattern is live, else the failure text the caller prints.
function Get-GuardProblem {
    param([int]$Actual, [int]$Minimum, [string]$Label, [string]$Suspected)
    if ($Actual -lt $Minimum) {
        return ('pattern failure (expected >= ' + $Minimum + ', got ' + $Actual + ') -- ' + $Suspected)
    }
    return $null
}

# ===========================================================================
# 0. dependency direction (why "cews -> coe" is not a defect)
#
#    The three content modules are separately installable jars.  cews and transmutation
#    declare  [[dependencies."<modid>"]]  modId = "createoreexpansion"  type = "required",
#    so FML refuses to load them without coe.  Their asset references into coe are therefore
#    always satisfied in every legal installation.  coe declares only third-party required
#    dependencies, so a reference from coe into cews survives only by accident.
#    Read mechanically; nothing here is a whitelist of accepted violations.
# ===========================================================================

# One documented identity table: Gradle project -> the modId that project publishes.
# core is a JarJar GAME LIBRARY and publishes no mod at all.  Cross-checked against the
# built jar's own [[mods]] block in A6 whenever a jar exists (see Get-JarDeclaredModId).
$ModuleModIds = [ordered]@{
    'root'          = 'coe_integration'
    'core'          = ''
    'coe'           = 'createoreexpansion'
    'cews'          = 'cews'
    'transmutation' = 'transmutation'
}
$TomlTemplateRel = 'src/main/templates/META-INF/neoforge.mods.toml'
$TomlJarEntry = 'META-INF/neoforge.mods.toml'

# A line-based TOML reader for exactly the shape FML templates use: a sequence of
#   [[dependencies.<owner>]]   blocks each carrying   modId = "..."   and   type = "...".
# Returns a list of @{ ModId; Type }.  Anything else in the file is ignored by design.
function Get-TomlDependencyBlocks {
    param([string]$Text)
    $out = New-Object System.Collections.Generic.List[object]
    $cur = $null
    $inDeps = $false
    if ($null -eq $Text) { return $out.ToArray() }
    foreach ($rawLine in ($Text -split "`r?`n")) {
        $line = $rawLine
        $hash = $line.IndexOf('#')
        if ($hash -ge 0) { $line = $line.Substring(0, $hash) }
        $line = $line.Trim()
        if ($line -eq '') { continue }
        if ($line.StartsWith('[[')) {
            if ($inDeps -and ($null -ne $cur)) { [void]$out.Add($cur) }
            $cur = $null
            $inDeps = $line.StartsWith('[[dependencies')
            if ($inDeps) { $cur = @{ ModId = ''; Type = '' } }
            continue
        }
        if ($line.StartsWith('[')) {
            if ($inDeps -and ($null -ne $cur)) { [void]$out.Add($cur) }
            $cur = $null
            $inDeps = $false
            continue
        }
        if (-not $inDeps -or ($null -eq $cur)) { continue }
        $eq = $line.IndexOf('=')
        if ($eq -lt 0) { continue }
        $key = $line.Substring(0, $eq).Trim()
        $val = $line.Substring($eq + 1).Trim().Trim('"')
        if ($key -ceq 'modId') { $cur.ModId = $val }
        elseif ($key -ceq 'type') { $cur.Type = $val }
    }
    if ($inDeps -and ($null -ne $cur)) { [void]$out.Add($cur) }
    return $out.ToArray()
}

# Reads <module>/src/main/templates/META-INF/neoforge.mods.toml and returns
# @{ RequiredModIds = string[]; File = <rel>; Found = bool; Required = <count of required> }
function Get-ModuleRequiredModIds {
    param([string]$ModuleDir)
    $p = Join-Path $ModuleDir $TomlTemplateRel
    $res = @{ RequiredModIds = @(); File = $TomlTemplateRel; Found = $false; Required = 0; Declared = 0; Local = @() }
    if (-not (Test-Path -LiteralPath $p)) { return $res }
    $res.Found = $true
    $text = Get-Content -LiteralPath $p -Raw -Encoding UTF8
    $blocks = @(Get-TomlDependencyBlocks $text)
    $res.Declared = $blocks.Count
    $ids = New-Object System.Collections.Generic.List[string]
    foreach ($b in $blocks) {
        if ($b.Type -cne 'required') { continue }
        if ($b.ModId -eq '') { continue }
        $res.Required = $res.Required + 1
        if (-not $ids.Contains($b.ModId)) { [void]$ids.Add($b.ModId) }
    }
    $res.RequiredModIds = $ids.ToArray()
    return $res
}

# module -> HashSet of modules it may legally pull assets from (transitive closure).
# Also returns the evidence row per module for the report.
function New-DependencyGraph {
    param([object[]]$Units, [string]$RepoRoot)
    $modIdToModule = @{}
    foreach ($m in $ModuleModIds.Keys) {
        $id = $ModuleModIds[$m]
        if ($id -ne '') { $modIdToModule[$id] = $m }
    }
    $direct = @{}
    $rows = New-Object System.Collections.Generic.List[object]
    foreach ($u in $Units) {
        $info = Get-ModuleRequiredModIds $u.Root
        $locals = New-Object System.Collections.Generic.List[string]
        foreach ($id in $info.RequiredModIds) {
            if ($modIdToModule.ContainsKey($id)) {
                $owner = $modIdToModule[$id]
                if ($owner -ne $u.Name) { [void]$locals.Add($owner) }
            }
        }
        $direct[$u.Name] = @($locals.ToArray())
        [void]$rows.Add(@{
            Module = $u.Name; OwnModId = $ModuleModIds[$u.Name]; File = $info.File
            Found = $info.Found; Declared = $info.Declared; Required = $info.Required
            RequiredModIds = $info.RequiredModIds; DirectLocal = @($locals.ToArray())
        })
    }
    # transitive closure (the graph is tiny; a fixed-point loop is enough and cannot loop
    # forever because the allowed set only grows)
    $allowed = @{}
    foreach ($m in $direct.Keys) {
        $set = New-Object System.Collections.Generic.HashSet[string]
        [void]$set.Add($m)
        [void]$allowed.Add($m, $set)
    }
    $changed = $true
    $rounds = 0
    while ($changed -and ($rounds -lt 32)) {
        $changed = $false
        $rounds++
        foreach ($m in $direct.Keys) {
            $before = $allowed[$m].Count
            foreach ($d in @($direct[$m])) {
                if (-not $allowed.ContainsKey($d)) { continue }
                foreach ($x in @($allowed[$d])) { [void]$allowed[$m].Add($x) }
            }
            if ($allowed[$m].Count -ne $before) { $changed = $true }
        }
    }
    return @{ Direct = $direct; Allowed = $allowed; Rows = $rows.ToArray() }
}

# The modId a built jar declares in its own [[mods]] block (A6 cross-check of the table).
function Get-JarDeclaredModId {
    param($Zip)
    foreach ($e in $Zip.Entries) {
        if ($e.FullName -cne $TomlJarEntry) { continue }
        $sr = New-Object System.IO.StreamReader($e.Open(), [System.Text.Encoding]::UTF8)
        try { $text = $sr.ReadToEnd() } finally { $sr.Dispose() }
        $m = [regex]::Match($text, '(?ms)^\s*\[\[mods\]\].*?^\s*modId\s*=\s*"([^"]+)"')
        if ($m.Success) { return $m.Groups[1].Value }
        return ''
    }
    return $null
}

# ===========================================================================
# 1. JSON reader (see PARSER NOTE in the header).
#    Returns: object -> Hashtable (string keys, so "" is a legal key),
#             array  -> object[] (an empty array degrades to $null, which every
#                       accessor below treats as "absent"), string -> string,
#             number -> double, true/false -> bool, null -> $null.
# ===========================================================================

function New-JsonState {
    param([string]$Text)
    return @{ Text = $Text; Pos = 0; Len = $Text.Length }
}

function Skip-JsonWs {
    param([hashtable]$S)
    while ($S.Pos -lt $S.Len) {
        $c = $S.Text[$S.Pos]
        if ($c -eq ' ' -or $c -eq "`t" -or $c -eq "`n" -or $c -eq "`r") { $S.Pos++ } else { break }
    }
}

function Read-JsonString {
    param([hashtable]$S)
    $S.Pos++                                   # opening quote
    $sb = New-Object System.Text.StringBuilder
    while ($true) {
        if ($S.Pos -ge $S.Len) { throw ('unterminated string at offset ' + $S.Pos) }
        $c = $S.Text[$S.Pos]
        if ($c -eq '"') { $S.Pos++; break }
        if ($c -eq '\') {
            $S.Pos++
            if ($S.Pos -ge $S.Len) { throw ('unterminated escape at offset ' + $S.Pos) }
            $e = [string]$S.Text[$S.Pos]
            if ($e -eq '"') { [void]$sb.Append('"') }
            elseif ($e -eq '\') { [void]$sb.Append('\') }
            elseif ($e -eq '/') { [void]$sb.Append('/') }
            elseif ($e -eq 'b') { [void]$sb.Append([char]8) }
            elseif ($e -eq 'f') { [void]$sb.Append([char]12) }
            elseif ($e -eq 'n') { [void]$sb.Append([char]10) }
            elseif ($e -eq 'r') { [void]$sb.Append([char]13) }
            elseif ($e -eq 't') { [void]$sb.Append([char]9) }
            elseif ($e -eq 'u') {
                if (($S.Pos + 4) -ge $S.Len) { throw ('truncated \\u escape at offset ' + $S.Pos) }
                $hex = $S.Text.Substring($S.Pos + 1, 4)
                $code = [Convert]::ToInt32($hex, 16)
                [void]$sb.Append([char]$code)
                $S.Pos = $S.Pos + 4
            } else {
                throw ('bad escape \' + $e + ' at offset ' + $S.Pos)
            }
            $S.Pos++
            continue
        }
        [void]$sb.Append($c)
        $S.Pos++
    }
    return $sb.ToString()
}

function Read-JsonNumber {
    param([hashtable]$S)
    $start = $S.Pos
    while ($S.Pos -lt $S.Len) {
        $c = $S.Text[$S.Pos]
        $isDigit = (($c -ge [char]'0') -and ($c -le [char]'9'))
        if ($isDigit -or $c -eq '-' -or $c -eq '+' -or $c -eq '.' -or $c -eq 'e' -or $c -eq 'E') {
            $S.Pos++
        } else { break }
    }
    $raw = $S.Text.Substring($start, $S.Pos - $start)
    if ($raw -eq '') { throw ('expected a number at offset ' + $start) }
    return [double]$raw
}

function Read-JsonValue {
    param([hashtable]$S)
    Skip-JsonWs $S
    if ($S.Pos -ge $S.Len) { throw ('unexpected end of input at offset ' + $S.Pos) }
    $c = $S.Text[$S.Pos]
    if ($c -eq '{') { return (Read-JsonObject $S) }
    if ($c -eq '[') { return (Read-JsonArray $S) }
    if ($c -eq '"') { return (Read-JsonString $S) }
    $rest = $S.Text.Substring($S.Pos)
    if ($rest.StartsWith('true'))  { $S.Pos = $S.Pos + 4; return $true }
    if ($rest.StartsWith('false')) { $S.Pos = $S.Pos + 5; return $false }
    if ($rest.StartsWith('null'))  { $S.Pos = $S.Pos + 4; return $null }
    return (Read-JsonNumber $S)
}

function Read-JsonObject {
    param([hashtable]$S)
    $o = @{}
    $S.Pos++
    Skip-JsonWs $S
    if (($S.Pos -lt $S.Len) -and ($S.Text[$S.Pos] -eq '}')) { $S.Pos++; return $o }
    while ($true) {
        Skip-JsonWs $S
        if (($S.Pos -ge $S.Len) -or ($S.Text[$S.Pos] -ne '"')) { throw ('expected an object key at offset ' + $S.Pos) }
        $k = Read-JsonString $S
        Skip-JsonWs $S
        if (($S.Pos -ge $S.Len) -or ($S.Text[$S.Pos] -ne ':')) { throw ('expected ":" at offset ' + $S.Pos) }
        $S.Pos++
        $v = Read-JsonValue $S
        $o[$k] = $v
        Skip-JsonWs $S
        if ($S.Pos -ge $S.Len) { throw ('unterminated object at offset ' + $S.Pos) }
        $ch = $S.Text[$S.Pos]
        if ($ch -eq ',') { $S.Pos++; continue }
        if ($ch -eq '}') { $S.Pos++; break }
        throw ('expected "," or "}" at offset ' + $S.Pos)
    }
    return $o
}

function Read-JsonArray {
    param([hashtable]$S)
    $list = New-Object System.Collections.Generic.List[object]
    $S.Pos++
    Skip-JsonWs $S
    if (($S.Pos -lt $S.Len) -and ($S.Text[$S.Pos] -eq ']')) { $S.Pos++; return $list.ToArray() }
    while ($true) {
        $v = Read-JsonValue $S
        [void]$list.Add($v)
        Skip-JsonWs $S
        if ($S.Pos -ge $S.Len) { throw ('unterminated array at offset ' + $S.Pos) }
        $ch = $S.Text[$S.Pos]
        if ($ch -eq ',') { $S.Pos++; continue }
        if ($ch -eq ']') { $S.Pos++; break }
        throw ('expected "," or "]" at offset ' + $S.Pos)
    }
    return $list.ToArray()
}

function ConvertFrom-JsonText {
    param([string]$Text)
    $S = New-JsonState $Text
    Skip-JsonWs $S
    $v = Read-JsonValue $S
    Skip-JsonWs $S
    if ($S.Pos -ne $S.Len) { throw ('trailing content at offset ' + $S.Pos) }
    return $v
}

# ---- structure accessors that work on both the hand-rolled and the platform shape ----

function Test-IsObject {
    param($v)
    if ($null -eq $v) { return $false }
    if ($v -is [hashtable]) { return $true }
    if ($v -is [System.Management.Automation.PSCustomObject]) { return $true }
    return $false
}

function Get-ObjectNames {
    param($o)
    if (-not (Test-IsObject $o)) { return @() }
    if ($o -is [hashtable]) { return @($o.Keys) }
    return @($o.PSObject.Properties.Name)
}

function Get-ObjectValue {
    param($o, [string]$name)
    if (-not (Test-IsObject $o)) { return $null }
    if ($o -is [hashtable]) {
        if ($o.ContainsKey($name)) { return $o[$name] }
        return $null
    }
    $p = $o.PSObject.Properties[$name]
    if ($null -eq $p) { return $null }
    return $p.Value
}

function Get-AsList {
    param($v)
    $out = New-Object System.Collections.Generic.List[object]
    if ($null -eq $v) { return $out.ToArray() }
    if ($v -is [System.Array]) {
        foreach ($x in $v) { [void]$out.Add($x) }
    } else {
        [void]$out.Add($v)
    }
    return $out.ToArray()
}

# ===========================================================================
# 2. resource id helpers
# ===========================================================================

# ResourceLocation.parse semantics: missing namespace means minecraft:, and both halves
# are lower-cased.  Returns a hashtable so the caller can see whether a namespace was
# written at all (a namespace-less id is never ours).
function Split-ResourceId {
    param([string]$Raw)
    $i = $Raw.IndexOf(':')
    if ($i -lt 0) {
        return @{ Namespace = 'minecraft'; Path = $Raw.ToLowerInvariant(); HasNamespace = $false }
    }
    return @{
        Namespace    = $Raw.Substring(0, $i).ToLowerInvariant()
        Path         = $Raw.Substring($i + 1).ToLowerInvariant()
        HasNamespace = $true
    }
}

# ===========================================================================
# 3. reference extraction  (one implementation, so the two parsers are really
#    compared on the same traversal -- A5)
# ===========================================================================

# Kind  = which resource space the target lives in: model | texture
# Shape = where in the JSON it was written (kept for the report and for the guards)
function Get-AssetReferences {
    param($Doc, [bool]$IsBlockstate)

    $refs = New-Object System.Collections.Generic.List[object]

    if ($IsBlockstate) {
        $variants = Get-ObjectValue $Doc 'variants'
        foreach ($k in (Get-ObjectNames $variants)) {
            foreach ($v in @(Get-AsList (Get-ObjectValue $variants $k))) {
                if (-not (Test-IsObject $v)) { continue }
                $m = Get-ObjectValue $v 'model'
                if ($m -is [string]) {
                    [void]$refs.Add((New-RefRecord 'model' 'blockstate.variants' $m))
                }
            }
        }
        foreach ($part in @(Get-AsList (Get-ObjectValue $Doc 'multipart'))) {
            if (-not (Test-IsObject $part)) { continue }
            foreach ($a in @(Get-AsList (Get-ObjectValue $part 'apply'))) {
                if (-not (Test-IsObject $a)) { continue }
                $m = Get-ObjectValue $a 'model'
                if ($m -is [string]) {
                    [void]$refs.Add((New-RefRecord 'model' 'blockstate.multipart' $m))
                }
            }
        }
        return $refs.ToArray()
    }

    $p = Get-ObjectValue $Doc 'parent'
    if ($p -is [string]) {
        [void]$refs.Add((New-RefRecord 'model' 'model.parent' $p))
    }
    $tex = Get-ObjectValue $Doc 'textures'
    if (Test-IsObject $tex) {
        foreach ($k in (Get-ObjectNames $tex)) {
            $v = Get-ObjectValue $tex $k
            if ($v -is [string]) {
                [void]$refs.Add((New-RefRecord 'texture' 'model.textures' $v))
            }
        }
    }
    foreach ($o in @(Get-AsList (Get-ObjectValue $Doc 'overrides'))) {
        if (-not (Test-IsObject $o)) { continue }
        $m = Get-ObjectValue $o 'model'
        if ($m -is [string]) {
            [void]$refs.Add((New-RefRecord 'model' 'model.overrides' $m))
        }
    }
    return $refs.ToArray()
}

function New-RefRecord {
    param([string]$Kind, [string]$Shape, [string]$Raw)
    $r = @{ Kind = $Kind; Shape = $Shape; Raw = $Raw; Variable = $false; Target = ''; Namespace = ''; Path = '' }
    if ($Raw.StartsWith('#')) {
        $r.Variable = $true
        $r.Namespace = ''
        $r.Path = $Raw.Substring(1)
        return $r
    }
    $id = Split-ResourceId $Raw
    $r.Namespace = $id.Namespace
    $r.Path = $id.Path
    if (($Kind -eq 'texture') -and $r.Path.EndsWith('.png')) {
        $r.Path = $r.Path.Substring(0, $r.Path.Length - 4)
    }
    $r.Target = $r.Namespace + ':' + $r.Path
    return $r
}

# ===========================================================================
# 4. build a "unit" per module (source tree) or per jar
#    unit = @{ Name; Source; Root; Files(list of file records); Models(hash); Textures(hash) }
#    file record = @{ Rel; AssetRel; AssetKind; AssetPath; Text; Doc; Refs; ParseError }
# ===========================================================================

$ModuleTable = [ordered]@{
    'root'          = '.'
    'core'          = 'core'
    'coe'           = 'coe'
    'cews'          = 'cews'
    'transmutation' = 'transmutation'
}
$ResourceRoots = @('src/generated/resources', 'src/main/resources')

function New-FileRecord {
    param([string]$Rel, [string]$Text)
    $rec = @{ Rel = $Rel; Text = $Text; AssetKind = ''; AssetPath = ''; Doc = $null; Refs = @(); ParseError = '' }
    $idx = $Rel.IndexOf($AssetBase)
    if ($idx -lt 0) { return $rec }
    $ar = $Rel.Substring($idx + $AssetBase.Length)
    $rec.AssetRel = $ar
    if ($ar.StartsWith('blockstates/') -and $ar.EndsWith('.json')) {
        $rec.AssetKind = 'blockstate'
        $rec.AssetPath = $ar.Substring(12, $ar.Length - 12 - 5)
        return $rec
    }
    if ($ar.StartsWith('models/') -and $ar.EndsWith('.json')) {
        $rec.AssetKind = 'model'
        $rec.AssetPath = $ar.Substring(7, $ar.Length - 7 - 5)
        return $rec
    }
    if ($ar.StartsWith('textures/') -and $ar.EndsWith('.png')) {
        $rec.AssetKind = 'texture'
        $rec.AssetPath = $ar.Substring(9, $ar.Length - 9 - 4)
        return $rec
    }
    return $rec
}

function Add-ParsedRefs {
    param($rec)
    if (($rec.AssetKind -ne 'blockstate') -and ($rec.AssetKind -ne 'model')) { return }
    try {
        $rec.Doc = ConvertFrom-JsonText $rec.Text
        $rec.Refs = @(Get-AssetReferences $rec.Doc ($rec.AssetKind -eq 'blockstate'))
    } catch {
        $rec.ParseError = $_.Exception.Message
    }
}

function New-SourceUnit {
    param([string]$Name, [string]$ModuleDir)
    $u = @{ Name = $Name; Source = 'tree'; Root = $ModuleDir; Files = @(); Models = @{}; Textures = @{} }
    $files = New-Object System.Collections.Generic.List[object]
    foreach ($rr in $ResourceRoots) {
        $base = Join-Path $ModuleDir $rr
        if (-not (Test-Path -LiteralPath $base)) { continue }
        foreach ($sub in @('blockstates', 'models')) {
            $dir = Join-Path $base ($AssetBase.TrimEnd('/') + '/' + $sub)
            if (-not (Test-Path -LiteralPath $dir)) { continue }
            foreach ($f in (Get-ChildItem -LiteralPath $dir -Recurse -File -Filter '*.json' | Sort-Object FullName)) {
                $rel = ($rr + '/' + $AssetBase + $sub + '/' + $f.FullName.Substring($dir.Length).TrimStart('\', '/')).Replace('\', '/')
                $rec = New-FileRecord $rel (Get-Content -LiteralPath $f.FullName -Raw -Encoding UTF8)
                Add-ParsedRefs $rec
                [void]$files.Add($rec)
            }
        }
        $tdir = Join-Path $base ($AssetBase.TrimEnd('/') + '/textures')
        if (Test-Path -LiteralPath $tdir) {
            foreach ($f in (Get-ChildItem -LiteralPath $tdir -Recurse -File -Filter '*.png' | Sort-Object FullName)) {
                $rel = ($rr + '/' + $AssetBase + 'textures/' + $f.FullName.Substring($tdir.Length).TrimStart('\', '/')).Replace('\', '/')
                [void]$files.Add((New-FileRecord $rel ''))
            }
        }
    }
    $u.Files = $files.ToArray()
    foreach ($rec in $u.Files) {
        if ($rec.AssetKind -eq 'model')   { $u.Models[$rec.AssetPath] = $rec }
        if ($rec.AssetKind -eq 'texture') { $u.Textures[$rec.AssetPath] = $rec }
    }
    return $u
}

function New-JarUnit {
    param([string]$Name, [string]$JarPath, $Zip)
    $u = @{ Name = $Name; Source = 'jar'; Root = $JarPath; Files = @(); Models = @{}; Textures = @{} }
    $files = New-Object System.Collections.Generic.List[object]
    foreach ($e in $Zip.Entries) {
        if ($e.FullName.EndsWith('/')) { continue }
        $n = $e.FullName
        if ($n -notlike ('*' + $AssetBase + '*')) { continue }
        $isJson = $n.EndsWith('.json') -and (($n -like ('*' + $AssetBase + 'blockstates/*')) -or ($n -like ('*' + $AssetBase + 'models/*')))
        $isPng = $n.EndsWith('.png') -and ($n -like ('*' + $AssetBase + 'textures/*'))
        if (-not ($isJson -or $isPng)) { continue }
        $text = ''
        if ($isJson) {
            $sr = New-Object System.IO.StreamReader($e.Open(), [System.Text.Encoding]::UTF8)
            try { $text = $sr.ReadToEnd() } finally { $sr.Dispose() }
        }
        $rec = New-FileRecord $n $text
        Add-ParsedRefs $rec
        [void]$files.Add($rec)
    }
    $u.Files = $files.ToArray()
    foreach ($rec in $u.Files) {
        if ($rec.AssetKind -eq 'model')   { $u.Models[$rec.AssetPath] = $rec }
        if ($rec.AssetKind -eq 'texture') { $u.Textures[$rec.AssetPath] = $rec }
    }
    return $u
}

# ===========================================================================
# 5. indexes and scanning
# ===========================================================================

function New-AssetIndex {
    param([object[]]$Units)
    $modelOwner = @{}
    $texOwner = @{}
    $dupModels = New-Object System.Collections.Generic.List[string]
    $dupTextures = New-Object System.Collections.Generic.List[string]
    foreach ($u in $Units) {
        foreach ($p in $u.Models.Keys) {
            if (-not $modelOwner.ContainsKey($p)) { $modelOwner[$p] = @() }
            $modelOwner[$p] = @($modelOwner[$p]) + @($u.Name)
        }
        foreach ($p in $u.Textures.Keys) {
            if (-not $texOwner.ContainsKey($p)) { $texOwner[$p] = @() }
            $texOwner[$p] = @($texOwner[$p]) + @($u.Name)
        }
    }
    foreach ($p in $modelOwner.Keys) { if (@($modelOwner[$p]).Count -gt 1) { [void]$dupModels.Add($p) } }
    foreach ($p in $texOwner.Keys)   { if (@($texOwner[$p]).Count -gt 1) { [void]$dupTextures.Add($p) } }
    return @{ ModelOwner = $modelOwner; TextureOwner = $texOwner; DupModels = $dupModels.ToArray(); DupTextures = $dupTextures.ToArray() }
}

function Get-Owners {
    param([hashtable]$Index, [string]$Kind, [string]$Path)
    if ($Kind -eq 'model') {
        if ($Index.ModelOwner.ContainsKey($Path)) { return @($Index.ModelOwner[$Path]) }
        return @()
    }
    if ($Index.TextureOwner.ContainsKey($Path)) { return @($Index.TextureOwner[$Path]) }
    return @()
}

# A1: every in-namespace reference written in a module's own asset JSON must resolve to a
# file inside that same module -- or inside a module that module declares as required.
# Runs over ALL asset JSON of the module (not only the anchor-reachable ones) so that
# orphan sidecars cannot hide a leak.
function Invoke-LocalityScan {
    param([object[]]$Units, [hashtable]$Index, [hashtable]$Allowed)

    $problems = New-Object System.Collections.Generic.List[object]
    $legalRefs = New-Object System.Collections.Generic.List[object]
    $counts = @{
        FilesJson = 0; FilesBlockstate = 0; RefsTotal = 0; RefsInScope = 0; RefsOutOfScope = 0
        Local = 0; LegalCross = 0; IllegalCross = 0; Dangling = 0; VariableRefs = 0
        BlockstateModel = 0; OverrideModel = 0; Parent = 0; Texture = 0; RefFiles = 0
        Namespaces = @{}
    }
    $reverseModel = @{}
    $reverseTexture = @{}
    if ($null -eq $Allowed) { $Allowed = @{} }

    foreach ($u in $Units) {
        foreach ($rec in $u.Files) {
            if ($rec.ParseError -ne '') { continue }
            if ($rec.AssetKind -ne 'blockstate' -and $rec.AssetKind -ne 'model') { continue }
            $counts['FilesJson'] = $counts['FilesJson'] + 1
            if ($rec.AssetKind -eq 'blockstate') { $counts['FilesBlockstate'] = $counts['FilesBlockstate'] + 1 }
            $sawInScope = $false
            foreach ($r in $rec.Refs) {
                $counts['RefsTotal'] = $counts['RefsTotal'] + 1
                if ($r.Variable) { $counts['VariableRefs'] = $counts['VariableRefs'] + 1; continue }
                # shape is the honest classifier: a parent reference IS a model id, so its
                # Kind is 'model'; only the Shape says where it was written.
                if ($r.Shape -eq 'model.parent')   { $counts['Parent'] = $counts['Parent'] + 1 }
                if ($r.Shape -eq 'model.textures') { $counts['Texture'] = $counts['Texture'] + 1 }
                if ($r.Shape -eq 'blockstate.variants' -or $r.Shape -eq 'blockstate.multipart') { $counts['BlockstateModel'] = $counts['BlockstateModel'] + 1 }
                if ($r.Shape -eq 'model.overrides') { $counts['OverrideModel'] = $counts['OverrideModel'] + 1 }
                if ($r.Namespace -ne $Namespace) {
                    $counts['RefsOutOfScope'] = $counts['RefsOutOfScope'] + 1
                    $nsTab = $counts['Namespaces']
                    if ($nsTab.ContainsKey($r.Namespace)) { $nsTab[$r.Namespace] = $nsTab[$r.Namespace] + 1 } else { $nsTab[$r.Namespace] = 1 }
                    continue
                }
                $counts['RefsInScope'] = $counts['RefsInScope'] + 1
                $sawInScope = $true
                # reverse index: which modules reference this id
                if ($r.Kind -eq 'model') {
                    if (-not $reverseModel.ContainsKey($r.Path)) { $reverseModel[$r.Path] = New-Object System.Collections.Generic.List[string] }
                    if (-not $reverseModel[$r.Path].Contains($u.Name)) { [void]$reverseModel[$r.Path].Add($u.Name) }
                } else {
                    if (-not $reverseTexture.ContainsKey($r.Path)) { $reverseTexture[$r.Path] = New-Object System.Collections.Generic.List[string] }
                    if (-not $reverseTexture[$r.Path].Contains($u.Name)) { [void]$reverseTexture[$r.Path].Add($u.Name) }
                }
                $owners = @(Get-Owners $Index $r.Kind $r.Path)
                if ($owners.Count -eq 0) {
                    $counts['Dangling'] = $counts['Dangling'] + 1
                    [void]$problems.Add((New-Problem $u.Name $rec.Rel $r 'DANGLING' @() ))
                } elseif ($owners -contains $u.Name) {
                    $counts['Local'] = $counts['Local'] + 1
                } else {
                    $licensed = $false
                    if ($Allowed.ContainsKey($u.Name)) {
                        foreach ($o in $owners) { if ($Allowed[$u.Name].Contains($o)) { $licensed = $true } }
                    }
                    if ($licensed) {
                        $counts['LegalCross'] = $counts['LegalCross'] + 1
                        [void]$legalRefs.Add((New-Problem $u.Name $rec.Rel $r 'LEGAL' $owners))
                    } else {
                        $counts['IllegalCross'] = $counts['IllegalCross'] + 1
                        [void]$problems.Add((New-Problem $u.Name $rec.Rel $r 'ILLEGAL' $owners))
                    }
                }
            }
            # "reference count > 0" alone is not enough: a large count can come from a single
            # file, i.e. a very narrow scan surface.  Count the distinct files that actually
            # produced an in-namespace reference and guard that number too.
            if ($sawInScope) { $counts['RefFiles'] = $counts['RefFiles'] + 1 }
        }
    }
    return @{
        Problems = $problems.ToArray(); LegalRefs = $legalRefs.ToArray(); Counts = $counts
        ReverseModel = $reverseModel; ReverseTexture = $reverseTexture
    }
}

function New-Problem {
    param([string]$Module, [string]$Rel, $Ref, [string]$Status, [object[]]$Owners)
    $ownerMod = '(nowhere in the repo)'
    if ($Owners.Count -gt 0) { $ownerMod = ($Owners -join '+') }
    return @{
        Module = $Module; Rel = $Rel; Kind = $Ref.Kind; Shape = $Ref.Shape
        Target = $Ref.Target; Status = $Status; Home = $ownerMod; Owners = $Owners; Reachable = $false
    }
}

# A4: walk the reference graph from a module's anchors, following targets wherever they
# resolve (across modules), and collect the files the closure needs but that live elsewhere.
function Get-ClosureVisit {
    param([string]$StartModule, [object[]]$Units, [hashtable]$Index)

    $byName = @{}
    foreach ($u in $Units) { $byName[$u.Name] = $u }

    $visitedModels = @{}
    $visitedTextures = @{}
    $queue = New-Object System.Collections.Generic.Queue[object]
    $anchorModelPaths = New-Object System.Collections.Generic.List[string]

    $startUnit = $byName[$StartModule]
    foreach ($rec in $startUnit.Files) {
        if ($rec.ParseError -ne '') { continue }
        if ($rec.AssetKind -eq 'blockstate') {
            foreach ($r in $rec.Refs) {
                if ($r.Variable) { continue }
                if ($r.Namespace -ne $Namespace) { continue }
                if ($r.Kind -eq 'model') { [void]$anchorModelPaths.Add($r.Path) }
            }
        } elseif (($rec.AssetKind -eq 'model') -and $rec.AssetRel.StartsWith('models/item/')) {
            [void]$anchorModelPaths.Add($rec.AssetPath)
        }
    }
    foreach ($p in $anchorModelPaths) { $queue.Enqueue(@{ Kind = 'model'; Path = $p }) }

    $seen = @{}
    while ($queue.Count -gt 0) {
        $item = $queue.Dequeue()
        $key = $item.Kind + '|' + $item.Path
        if ($seen.ContainsKey($key)) { continue }
        $seen[$key] = $true
        $owners = @(Get-Owners $Index $item.Kind $item.Path)
        if ($owners.Count -eq 0) { continue }
        $ownerMod = $owners[0]
        if (-not $byName.ContainsKey($ownerMod)) { continue }
        $hrec = $null
        if ($item.Kind -eq 'model') {
            if (-not $byName[$ownerMod].Models.ContainsKey($item.Path)) { continue }
            $hrec = $byName[$ownerMod].Models[$item.Path]
            $visitedModels[$item.Path] = $ownerMod
        } else {
            if (-not $byName[$ownerMod].Textures.ContainsKey($item.Path)) { continue }
            $hrec = $byName[$ownerMod].Textures[$item.Path]
            $visitedTextures[$item.Path] = $ownerMod
        }
        if ($hrec.ParseError -ne '') { continue }
        foreach ($r in $hrec.Refs) {
            if ($r.Variable) { continue }
            if ($r.Namespace -ne $Namespace) { continue }
            $queue.Enqueue(@{ Kind = $r.Kind; Path = $r.Path })
        }
    }
    return @{ Models = $visitedModels; Textures = $visitedTextures }
}

# Owner-directory rollup: how many asset files a module provides per (kind, directory),
# so a migration can be told "files=7 needed=5 orphan=2" instead of a bare reference count.
function Register-Dir {
    param([hashtable]$Table, [string]$OwnerMod, [string]$Kind, [string]$Path)
    $dir = ''
    $i = $Path.LastIndexOf('/')
    if ($i -ge 0) { $dir = $Path.Substring(0, $i) }
    $k = $OwnerMod + '|' + $Kind + '|' + $dir
    if (-not $Table.ContainsKey($k)) {
        $Table[$k] = @{ Home = $OwnerMod; Kind = $Kind; Dir = $dir; Files = 0; NeededLegal = 0; NeededIllegal = 0; Referers = (New-Object System.Collections.Generic.List[string]) }
    }
    $e = $Table[$k]
    $e.Files = $e.Files + 1
}

# Cross-module closure entry, grouped by (referencing module, owner, kind, legality).
function Add-Strand {
    param([hashtable]$Table, [string]$Ref, [string]$OwnerMod, [string]$Kind, [string]$Path, [bool]$Legal)
    $k = $Ref + '|' + $OwnerMod + '|' + $Kind
    if (-not $Table.ContainsKey($k)) {
        $Table[$k] = @{ Ref = $Ref; Home = $OwnerMod; Kind = $Kind; Legal = $Legal; Count = 0; Samples = (New-Object System.Collections.Generic.List[string]); Files = (New-Object System.Collections.Generic.HashSet[string]) }
    }
    $e = $Table[$k]
    $e.Count = $e.Count + 1
    [void]$e.Files.Add($Path)
    if ($e.Samples.Count -lt 4) { [void]$e.Samples.Add($Namespace + ':' + $Path) }
}

# ===========================================================================
# 6. gather the working tree
# ===========================================================================

Write-Host '=========================================================================='
Write-Host 'check-asset-attribution.ps1 -- per-module asset attribution gate'
Write-Host ('repo: ' + $RepoRoot)
Write-Host '=========================================================================='
Write-Host ''

Write-Host '[A] working-tree asset inventory'
$units = New-Object System.Collections.Generic.List[object]
foreach ($m in $ModuleTable.Keys) {
    $dir = Join-Path $RepoRoot $ModuleTable[$m]
    if (-not (Test-Path -LiteralPath $dir)) {
        Write-Host ('        module ' + $m + ': MISSING directory ' + $ModuleTable[$m])
        continue
    }
    [void]$units.Add((New-SourceUnit $m $dir))
}
$units = $units.ToArray()
$index = New-AssetIndex $units

foreach ($u in $units) {
    $nBs = 0; $nItem = 0; $nModel = 0; $nTex = 0
    foreach ($rec in $u.Files) {
        if ($rec.AssetKind -eq 'blockstate') { $nBs++ }
        elseif ($rec.AssetKind -eq 'model') { $nModel++; if ($rec.AssetRel.StartsWith('models/item/')) { $nItem++ } }
        elseif ($rec.AssetKind -eq 'texture') { $nTex++ }
    }
    Write-Sub (('{0,-14} blockstates={1,-4} item-anchors={2,-4} models={3,-4} textures={4,-4} roots={5}' -f `
        $u.Name, $nBs, $nItem, $nModel, $nTex, (@($ResourceRoots | Where-Object { Test-Path -LiteralPath (Join-Path $u.Root $_) }) -join ',')))
}
Write-Host ''

$parseFailures = New-Object System.Collections.Generic.List[string]
$assetJsonCount = 0
$upperNames = New-Object System.Collections.Generic.List[string]
foreach ($u in $units) {
    foreach ($rec in $u.Files) {
        if ($rec.ParseError -ne '') { [void]$parseFailures.Add($u.Name + ' :: ' + $rec.Rel + ' :: ' + $rec.ParseError) }
        if ($rec.AssetKind -eq 'blockstate' -or $rec.AssetKind -eq 'model') { $assetJsonCount++ }
        if ($rec.Rel -cmatch '[A-Z]') { [void]$upperNames.Add($u.Name + ' :: ' + $rec.Rel) }
    }
}

# ===========================================================================
# 7. A1 attribution scan
# ===========================================================================

Write-Host '[B] declared dependency direction (read from neoforge.mods.toml, never guessed)'
$depGraph = New-DependencyGraph $units $RepoRoot
foreach ($row in $depGraph.Rows) {
    $own = $row.OwnModId
    if ($own -eq '') { $own = '(no mod)' }
    if (-not $row.Found) {
        Write-Sub (('{0,-14} modId={1,-20} template MISSING -> requires no local module (allowed set = itself)' -f $row.Module, $own))
        continue
    }
    $directText = '(none)'
    if (@($row.DirectLocal).Count -gt 0) { $directText = (@($row.DirectLocal) -join '+') }
    $allowedText = (@($depGraph.Allowed[$row.Module]) -join '+')
    Write-Sub (('{0,-14} modId={1,-20} declared={2,-3} required={3,-3} requires(local)={4,-8} can-use={5}' -f `
        $row.Module, $own, $row.Declared, $row.Required, $directText, $allowedText))
}
Write-Host ''

Write-Host '[C] reference census (working tree)'
foreach ($u in $units) {
    $bs = 0; $ov = 0; $pa = 0; $tx = 0
    foreach ($rec in $u.Files) {
        foreach ($r in $rec.Refs) {
            if ($r.Variable) { continue }
            if ($r.Shape -eq 'blockstate.variants' -or $r.Shape -eq 'blockstate.multipart') { $bs++ }
            if ($r.Shape -eq 'model.overrides') { $ov++ }
            if ($r.Shape -eq 'model.parent') { $pa++ }
            if ($r.Shape -eq 'model.textures') { $tx++ }
        }
    }
    Write-Sub (('{0,-14} blockstate.model={1,-5} model.parent={2,-5} model.textures={3,-5} model.overrides={4,-4}' -f $u.Name, $bs, $pa, $tx, $ov))
}
Write-Host ''

$scan = Invoke-LocalityScan $units $index $depGraph.Allowed
$c = $scan.Counts
$problems = @($scan.Problems)
$legalRefs = @($scan.LegalRefs)

$nsParts = @()
foreach ($k in ($c.Namespaces.Keys | Sort-Object)) { $nsParts = $nsParts + ($k + ':' + $c.Namespaces[$k]) }
Write-Host '[D] A1 asset attribution (dependency direction aware)'
Write-Sub ('asset JSON parsed: ' + $c.FilesJson + ' (blockstates ' + $c.FilesBlockstate + '); parse failures: ' + $parseFailures.Count)
Write-Sub ('references: total=' + $c.RefsTotal + ' in-namespace=' + $c.RefsInScope + ' out-of-scope=' + $c.RefsOutOfScope + ' [' + ($nsParts -join ', ') + '] variable(#name)=' + $c.VariableRefs)
Write-Sub ('in-namespace resolution: local=' + $c.Local + ' legal-cross-module=' + $c.LegalCross + ' illegal-direction=' + $c.IllegalCross + ' dangling=' + $c.Dangling)
Write-Sub ('distinct files that produced an in-namespace reference: ' + $c.RefFiles + ' of ' + $c.FilesJson + ' asset JSON files')
Write-Sub 'legend: this gate asserts "what a jar references resolves inside that jar (or inside a'
Write-Sub '        module it declares as required)".  It does NOT assert that a resource may only be'
Write-Sub '        referenced by its own module -- one texture shared by many models of another module'
Write-Sub '        is exactly what legal-cross-module counts, and it is not a defect.'
Write-Host ''

foreach ($f in $parseFailures) { Write-Sub ('PARSE FAILURE ' + $f) }

# dedupe the failure list on (module, file, kind, target): a blockstate repeats the same
# model id once per variant, and the reader wants one line per distinct missing asset.
$uniq = @{}
foreach ($p in $problems) {
    $k = $p.Module + '|' + $p.Rel + '|' + $p.Kind + '|' + $p.Target + '|' + $p.Status
    if (-not $uniq.ContainsKey($k)) { $uniq[$k] = $p }
}
$uniqProblems = @($uniq.Values | Sort-Object { $_.Module + '|' + $_.Rel + '|' + $_.Kind + '|' + $_.Target })

$attributionOk = ($uniqProblems.Count -eq 0)
Write-Check $attributionOk 'A1 every in-namespace asset reference resolves inside its own module or a declared-required module' `
    $(if ($attributionOk) {
        ('illegal=0 dangling=0 (local=' + $c.Local + ' legal-cross=' + $c.LegalCross + ' of ' + $c.RefsInScope + ' in-namespace reference sites)')
    } else {
        ('illegal/dangling reference sites=' + $problems.Count + ' (distinct file/kind/target=' + $uniqProblems.Count + '); see the list below')
    })
if (-not $attributionOk) {
    Write-Sub 'ILLEGAL / DANGLING references (referencing file | kind | target id | where the bytes actually are):'
    $shown = 0
    foreach ($p in $uniqProblems) {
        if ($shown -ge 400) { Write-Sub ('... and ' + ($uniqProblems.Count - 400) + ' more'); break }
        Write-Sub (('  {0} :: {1} | {2} | {3} | lives in: {4}' -f $p.Module, $p.Rel, $p.Kind, $p.Target, $p.Home))
        $shown++
    }
}
Write-Host ''

# ===========================================================================
# 8. A2 duplicate provider
# ===========================================================================

$dupModelCount = @($index.DupModels).Count
$dupTexCount = @($index.DupTextures).Count
# Same module, same asset id, from BOTH resource roots: this script's index is keyed by
# id, so the second copy would silently replace the first, and processResources would
# report a duplicate (or, worse, one copy would win in the jar).  Detected here because a
# silent index overwrite is exactly the class of hole this script exists to close.
$sameModuleDup = New-Object System.Collections.Generic.List[string]
foreach ($u in $units) {
    if ($u.Source -ne 'tree') { continue }
    $seenRel = @{}
    foreach ($rec in $u.Files) {
        if ($rec.AssetKind -eq '') { continue }
        if ($seenRel.ContainsKey($rec.AssetRel)) {
            [void]$sameModuleDup.Add($u.Name + ' :: ' + $rec.AssetRel + ' [' + $seenRel[$rec.AssetRel] + ' AND ' + $rec.Rel + ']')
        } else {
            $seenRel[$rec.AssetRel] = $rec.Rel
        }
    }
}
$dupOk = ((($dupModelCount + $dupTexCount) -eq 0) -and ($sameModuleDup.Count -eq 0))
Write-Check $dupOk 'A2 no in-namespace asset id is provided twice (across modules, or by two resource roots of one module)' `
    $(if ($dupOk) {
        ('duplicates=0 (models=' + $index.ModelOwner.Count + ' textures=' + $index.TextureOwner.Count + ' distinct ids across ' + $units.Count + ' modules; same-module double-root=0)')
    } else {
        ('duplicate models=' + $dupModelCount + ' duplicate textures=' + $dupTexCount + ' same-module double-root=' + $sameModuleDup.Count + ' :: ' + (((@($index.DupModels) + @($index.DupTextures) + @($sameModuleDup)) | Select-Object -First 20) -join ', '))
    })
Write-Host ''

# ===========================================================================
# 9. A4 cross-module asset inventory (legal half reported, illegal half scored)
# ===========================================================================

Write-Host '[E] A4 cross-module asset inventory'
Write-Sub 'A cross-module need is LEGAL when the owning module is in the referencing module''s declared allowed set.'

$visitedAll = @{}
$strand = @{}          # "referencing|owner|kind" -> @{ Ref; Home; Kind; Count; Samples; Files; Legal }
$dirRollup = @{}       # "owner|kind|dir" -> @{ Files; NeededLegal; NeededIllegal; Referers }

foreach ($u in $units) { $visitedAll[$u.Name] = Get-ClosureVisit $u.Name $units $index }

# owner-directory rollup straight from the index: total files per (module, kind, dir)
foreach ($u in $units) {
    foreach ($p in $u.Models.Keys)   { Register-Dir $dirRollup $u.Name 'model' $p }
    foreach ($p in $u.Textures.Keys) { Register-Dir $dirRollup $u.Name 'texture' $p }
}

function Test-IsLegalNeed {
    param([string]$RefModule, [string]$OwnerMod)
    if ($RefModule -eq $OwnerMod) { return $true }
    if (-not $depGraph.Allowed.ContainsKey($RefModule)) { return $false }
    return $depGraph.Allowed[$RefModule].Contains($OwnerMod)
}

foreach ($m in $units.Name) {
    $vis = $visitedAll[$m]
    foreach ($p in $vis.Models.Keys) {
        $ownerMod = $vis.Models[$p]
        if ($ownerMod -eq $m) { continue }
        Add-Strand $strand $m $ownerMod 'model' $p (Test-IsLegalNeed $m $ownerMod)
    }
    foreach ($p in $vis.Textures.Keys) {
        $ownerMod = $vis.Textures[$p]
        if ($ownerMod -eq $m) { continue }
        Add-Strand $strand $m $ownerMod 'texture' $p (Test-IsLegalNeed $m $ownerMod)
    }
}

# per-directory "needed" counts (deduped by path inside the closure)
$needSet = @{}
foreach ($m in $units.Name) {
    $vis = $visitedAll[$m]
    foreach ($p in $vis.Models.Keys) {
        $ownerMod = $vis.Models[$p]
        if ($ownerMod -ne $m) { $needSet[$ownerMod + '|model|' + $p] = $m }
    }
    foreach ($p in $vis.Textures.Keys) {
        $ownerMod = $vis.Textures[$p]
        if ($ownerMod -ne $m) { $needSet[$ownerMod + '|texture|' + $p] = $m }
    }
}
foreach ($k in $needSet.Keys) {
    $parts = $k.Split('|')
    $ownerMod = $parts[0]; $kind = $parts[1]; $path = $parts[2]
    $ref = $needSet[$k]
    $dir = ''
    $i = $path.LastIndexOf('/')
    if ($i -ge 0) { $dir = $path.Substring(0, $i) }
    $key = $ownerMod + '|' + $kind + '|' + $dir
    if ($dirRollup.ContainsKey($key)) {
        $e = $dirRollup[$key]
        if (Test-IsLegalNeed $ref $ownerMod) { $e.NeededLegal = $e.NeededLegal + 1 } else { $e.NeededIllegal = $e.NeededIllegal + 1 }
        if (-not $e.Referers.Contains($ref)) { [void]$e.Referers.Add($ref) }
    }
}

$legalTotal = 0
$legalFiles = New-Object System.Collections.Generic.HashSet[string]
$illegalTotal = 0
$illegalFiles = New-Object System.Collections.Generic.HashSet[string]
$legalLines = @()
$illegalLines = @()
foreach ($k in ($strand.Keys | Sort-Object)) {
    $e = $strand[$k]
    $line = ('{0,-14} {1,-14} {2,-8} {3,-6} {4}' -f $e.Ref, $e.Home, $e.Kind, $e.Count, ($e.Samples -join ', '))
    if ($e.Legal) {
        $legalTotal = $legalTotal + $e.Count
        foreach ($p in $e.Files) { [void]$legalFiles.Add($e.Home + '|' + $e.Kind + '|' + $p) }
        $legalLines = $legalLines + $line
    } else {
        $illegalTotal = $illegalTotal + $e.Count
        foreach ($p in $e.Files) { [void]$illegalFiles.Add($e.Home + '|' + $e.Kind + '|' + $p) }
        $illegalLines = $illegalLines + $line
    }
}
Write-Sub ('referencing    owner          kind     count  sample target ids')
if ($legalLines.Count -eq 0) {
    Write-Sub '  LEGAL: none (no module reaches into a module it declares as required).'
} else {
    Write-Sub '  ---- LEGAL cross-module needs (licensed by a required dependency; NOT a defect) ----'
    foreach ($l in $legalLines) { Write-Sub ('  ' + $l) }
    Write-Sub ('  legal total: ' + $legalTotal + ' closure entr(ies); distinct files: ' + $legalFiles.Count)
}
if ($illegalLines.Count -eq 0) {
    Write-Sub '  ILLEGAL: none (no module needs an asset from a module it does not require).'
} else {
    Write-Sub '  ---- ILLEGAL cross-module needs (no dependency licenses them; these break a single-module install) ----'
    foreach ($l in $illegalLines) { Write-Sub ('  ' + $l) }
    Write-Sub ('  illegal total: ' + $illegalTotal + ' closure entr(ies); distinct files stranded: ' + $illegalFiles.Count)
}
Write-Host ''

# owner-directory detail for the directories that appear above
$involvedDirs = @{}
foreach ($k in $dirRollup.Keys) {
    $e = $dirRollup[$k]
    if (($e.NeededLegal + $e.NeededIllegal) -gt 0) { $involvedDirs[$k] = $e }
}
if ($involvedDirs.Count -eq 0) {
    Write-Sub 'no owner directory is needed by another module; nothing to move.'
} else {
    Write-Sub 'owner-directory detail (files | needed legally | needed illegally | orphan sidecars | needed by):'
    $totFiles = 0; $totIllegal = 0
    foreach ($k in ($involvedDirs.Keys | Sort-Object)) {
        $e = $involvedDirs[$k]
        $totFiles = $totFiles + $e.Files
        $totIllegal = $totIllegal + $e.NeededIllegal
        $shown = $e.Dir
        if ($shown -eq '') { $shown = '(root)' }
        Write-Sub (('  {0,-14} {1,-9} {2,-34} files={3,-4} legal={4,-4} illegal={5,-4} orphan={6,-4} by={7}' -f `
            $e.Home, $e.Kind, $shown, $e.Files, $e.NeededLegal, $e.NeededIllegal, ($e.Files - $e.NeededLegal - $e.NeededIllegal), ($e.Referers -join '+')))
    }
    Write-Sub ('TOTAL files in the affected owner directories: ' + $totFiles + ' ; forced by this gate (illegal): ' + $totIllegal + ' ; orphan sidecars: ' + ($totFiles - $totIllegal - $legalFiles.Count))
}
Write-Host ''

# ---------------------------------------------------------------------------
# orphan classification.  An "orphan" is an asset id that NO module's anchor closure
# reaches.  This is REPORTED, never scored -- and it must never be promoted to an
# assertion, because two perfectly healthy shapes are statically invisible:
#   * Create's connected-texture naming convention: the engine COMPOSES "<name>_connected"
#     at run time, so {jade,sapphire,stellarstone}_casing_connected.png appear in no
#     `textures` field anywhere.  Requiring "every texture is referenced" would be
#     permanently red on a correct tree.
#   * ids a Java-side renderer/item builds by string concatenation (W9 measured 5 such
#     registrations, all in coe, ownership correct) also leave no JSON reference.
# So the orphans are bucketed and counted, and the reader can tell "CTM/naming-convention"
# from "genuinely unreferenced" without the gate ever failing on either.
# ---------------------------------------------------------------------------
$neededAny = New-Object System.Collections.Generic.HashSet[string]
foreach ($m in $visitedAll.Keys) {
    $vis = $visitedAll[$m]
    foreach ($p in $vis.Models.Keys)   { [void]$neededAny.Add($vis.Models[$p] + '|model|' + $p) }
    foreach ($p in $vis.Textures.Keys) { [void]$neededAny.Add($vis.Textures[$p] + '|texture|' + $p) }
}
$orphanCtm = New-Object System.Collections.Generic.List[string]
$orphanOther = New-Object System.Collections.Generic.List[string]
$orphanCtmTotal = 0
$orphanOtherTotal = 0
$orphanModelTotal = 0
foreach ($u in $units) {
    foreach ($rec in $u.Files) {
        if ($rec.AssetKind -eq 'model') {
            if ($neededAny.Contains($u.Name + '|model|' + $rec.AssetPath)) { continue }
            $orphanModelTotal++
            $orphanOtherTotal++
            if ($orphanOther.Count -lt 8) { [void]$orphanOther.Add('model  ' + $u.Name + ' :: ' + $rec.Rel) }
        } elseif ($rec.AssetKind -eq 'texture') {
            if ($neededAny.Contains($u.Name + '|texture|' + $rec.AssetPath)) { continue }
            $base = $rec.AssetPath
            $slash = $base.LastIndexOf('/')
            if ($slash -ge 0) { $base = $base.Substring($slash + 1) }
            if ($base.EndsWith('_connected')) {
                $orphanCtmTotal++
                if ($orphanCtm.Count -lt 8) { [void]$orphanCtm.Add($u.Name + ' :: ' + $rec.Rel) }
            } else {
                $orphanOtherTotal++
                if ($orphanOther.Count -lt 8) { [void]$orphanOther.Add('texture ' + $u.Name + ' :: ' + $rec.Rel) }
            }
        }
    }
}
Write-Sub ('orphans (reported, never scored): models not reached by any anchor chain=' + $orphanModelTotal)
Write-Sub ('  textures named by the CTM convention ("*_connected", composed at run time)=' + $orphanCtmTotal + ' -- these can never appear in a textures field, so an "every texture must be referenced" rule would be permanently red')
foreach ($o in $orphanCtm) { Write-Sub ('    ctm   ' + $o) }
Write-Sub ('  all other unreferenced assets=' + ($orphanOtherTotal - $orphanModelTotal) + ' textures + ' + $orphanModelTotal + ' models = ' + $orphanOtherTotal + ' (a candidate dead file, or an id a Java-side renderer builds by concatenation)')
foreach ($o in $orphanOther) { Write-Sub ('    other ' + $o) }
Write-Host ''

# ===========================================================================
# 10. A3 texture-variable resolvability (bounded)
# ===========================================================================

Write-Host '[F] A3 texture-variable (#name) resolvability'
$tvProblems = New-Object System.Collections.Generic.List[string]
$tvSites = 0
$tvExempt = 0

foreach ($u in $units) {
    foreach ($rec in $u.Files) {
        if ($rec.AssetKind -ne 'model' -or $rec.ParseError -ne '') { continue }
        # collect the texture variables visible to this model: itself plus every ancestor,
        # but only as long as every ancestor is a model this script can actually read.
        $defined = @{}
        $chainOpen = $false
        $cur = $rec
        $guard = 0
        while ($null -ne $cur) {
            $guard++
            if ($guard -gt 64) { $chainOpen = $true; break }
            $tex = Get-ObjectValue $cur.Doc 'textures'
            foreach ($nm in (Get-ObjectNames $tex)) { $defined[$nm] = $true }
            $pr = Get-ObjectValue $cur.Doc 'parent'
            if (-not ($pr -is [string])) { $cur = $null; break }
            $id = Split-ResourceId $pr
            if ($id.Namespace -ne $Namespace) { $chainOpen = $true; break }
            $owners = @(Get-Owners $index 'model' $id.Path)
            if ($owners.Count -eq 0) { $chainOpen = $true; break }
            $ownerMod = $owners[0]
            $nxt = $null
            foreach ($uu in $units) { if ($uu.Name -eq $ownerMod) { if ($uu.Models.ContainsKey($id.Path)) { $nxt = $uu.Models[$id.Path] } } }
            if ($null -eq $nxt -or $nxt.ParseError -ne '') { $chainOpen = $true; break }
            $cur = $nxt
        }
        # every "#name" used in this model's own textures map or in its element faces
        $uses = New-Object System.Collections.Generic.List[object]
        $tex = Get-ObjectValue $rec.Doc 'textures'
        foreach ($nm in (Get-ObjectNames $tex)) {
            $v = Get-ObjectValue $tex $nm
            if (($v -is [string]) -and $v.StartsWith('#')) { [void]$uses.Add(@{ Name = $v.Substring(1); Where = 'textures.' + $nm }) }
        }
        foreach ($el in @(Get-AsList (Get-ObjectValue $rec.Doc 'elements'))) {
            if (-not (Test-IsObject $el)) { continue }
            $faces = Get-ObjectValue $el 'faces'
            foreach ($fn in (Get-ObjectNames $faces)) {
                $fv = Get-ObjectValue (Get-ObjectValue $faces $fn) 'texture'
                if (($fv -is [string]) -and $fv.StartsWith('#')) { [void]$uses.Add(@{ Name = $fv.Substring(1); Where = 'elements.faces.' + $fn }) }
            }
        }
        foreach ($use in $uses) {
            $tvSites++
            if ($defined.ContainsKey($use.Name)) { continue }
            if ($chainOpen) { $tvExempt++; continue }
            [void]$tvProblems.Add(($u.Name + ' :: ' + $rec.Rel + ' | #' + $use.Name + ' | used at ' + $use.Where + ' | not defined by the model or any ancestor'))
        }
    }
}
$tvOk = ($tvProblems.Count -eq 0)
Write-Check $tvOk 'A3 every texture variable is defined by the model or a readable ancestor' `
    $(if ($tvOk) {
        ('sites=' + $tvSites + ' undefined=0 (exempt because the parent chain leaves the readable set=' + $tvExempt + ')')
    } else {
        ('undefined=' + $tvProblems.Count + ' of ' + $tvSites + ' sites :: ' + (($tvProblems | Select-Object -First 10) -join ' ;; '))
    })
Write-Host ''

# ===========================================================================
# 11. A5 parser self-check
# ===========================================================================

Write-Host '[G] A5 parser self-check (hand-rolled reader vs ConvertFrom-Json)'
$agree = 0
$cfjFail = 0
$mismatch = New-Object System.Collections.Generic.List[string]
foreach ($u in $units) {
    foreach ($rec in $u.Files) {
        if ($rec.AssetKind -ne 'blockstate' -and $rec.AssetKind -ne 'model') { continue }
        $doc2 = $null
        try { $doc2 = $rec.Text | ConvertFrom-Json } catch { $cfjFail++; continue }
        $refs2 = @()
        try { $refs2 = @(Get-AssetReferences $doc2 ($rec.AssetKind -eq 'blockstate')) } catch { [void]$mismatch.Add($u.Name + ' :: ' + $rec.Rel + ' :: traversal of the ConvertFrom-Json shape failed'); continue }
        $a = @($rec.Refs | ForEach-Object { if ($_.Variable) { '#' + $_.Path } else { $_.Kind + '=' + $_.Target } } | Sort-Object)
        $b = @($refs2 | ForEach-Object { if ($_.Variable) { '#' + $_.Path } else { $_.Kind + '=' + $_.Target } } | Sort-Object)
        if (($a -join "`n") -ceq ($b -join "`n")) { $agree++ }
        else { [void]$mismatch.Add(($u.Name + ' :: ' + $rec.Rel + ' :: mine=' + $a.Count + ' platform=' + $b.Count)) }
    }
}
$pCheckOk = ($mismatch.Count -eq 0) -and ($agree -ge 100)
Write-Check $pCheckOk 'A5 both JSON readers extract the identical reference list' `
    $(if ($mismatch.Count -ne 0) {
        ('mismatches=' + $mismatch.Count + ' :: ' + (($mismatch | Select-Object -First 10) -join ' ;; '))
    } elseif ($agree -lt 100) {
        ('pattern failure (expected >= 100 files both readers can read, got ' + $agree + ') -- the readers are not being exercised')
    } else {
        ('files agreed=' + $agree + ' ; files ConvertFrom-Json cannot read at all=' + $cfjFail + ' (empty-string JSON keys -- handled by the hand-rolled reader)')
    })
Write-Host ''

# ===========================================================================
# 12. A0 anti-vacuity guards
# ===========================================================================

Write-Host '[H] A0 anti-vacuity guards'
$guards = New-Object System.Collections.Generic.List[object]
[void]$guards.Add(@{ Label = 'asset JSON files scanned'; Actual = $assetJsonCount; Min = 100; Suspected = 'the asset walk found almost nothing; check the module table and the resource roots' })
[void]$guards.Add(@{ Label = 'blockstate anchors'; Actual = $c.FilesBlockstate; Min = 10; Suspected = 'blockstates/*.json were not collected' })
$itemAnchors = 0
foreach ($u in $units) { foreach ($rec in $u.Files) { if ($rec.AssetKind -eq 'model' -and $rec.AssetRel.StartsWith('models/item/')) { $itemAnchors++ } } }
[void]$guards.Add(@{ Label = 'models/item anchors'; Actual = $itemAnchors; Min = 10; Suspected = 'models/item/*.json were not collected' })
[void]$guards.Add(@{ Label = 'in-namespace references'; Actual = $c.RefsInScope; Min = 100; Suspected = 'the namespace filter is eating our own ids' })
[void]$guards.Add(@{ Label = 'distinct files that produced an in-namespace reference'; Actual = $c.RefFiles; Min = 100; Suspected = 'a large reference count coming from very few files means the scan surface is narrow, not that the tree is clean' })
$orphanWalkInput = 0
foreach ($u in $units) { $orphanWalkInput = $orphanWalkInput + $u.Models.Count + $u.Textures.Count }
# NOTE: the orphan COUNT itself is deliberately not guarded.  0 orphans is a legitimate tree,
# and demanding "at least one orphan" would be a permanently-red guard (the very anti-pattern
# the *_connected note above exists to prevent).  What is guarded is that the walk had input.
[void]$guards.Add(@{ Label = 'assets considered by the orphan walk (models+textures)'; Actual = $orphanWalkInput; Min = 100; Suspected = 'the orphan walk has no input, so the CTM classification below is unverified' })
[void]$guards.Add(@{ Label = 'blockstate -> model references'; Actual = $c.BlockstateModel; Min = 10; Suspected = 'blockstate variants/multipart are not being read' })
[void]$guards.Add(@{ Label = 'model -> parent references'; Actual = $c.Parent; Min = 10; Suspected = 'model parent is not being read' })
[void]$guards.Add(@{ Label = 'model -> texture references'; Actual = $c.Texture; Min = 100; Suspected = 'model textures are not being read' })
[void]$guards.Add(@{ Label = 'model -> overrides references'; Actual = $c.OverrideModel; Min = 1; Suspected = 'overrides[].model is not being read (the bow and the wave gauge depend on it)' })
[void]$guards.Add(@{ Label = 'out-of-scope references (minecraft:/create:/...)'; Actual = $c.RefsOutOfScope; Min = 10; Suspected = 'third-party filtering is dead; the namespace rule is not being applied' })
[void]$guards.Add(@{ Label = 'texture-variable sites (#name)'; Actual = $tvSites; Min = 10; Suspected = 'elements[].faces[].texture is not being read' })
$modulesWithAssets = 0
foreach ($u in $units) { if ($u.Models.Count -gt 0 -or $u.Textures.Count -gt 0) { $modulesWithAssets++ } }
[void]$guards.Add(@{ Label = 'modules carrying at least one model or texture'; Actual = $modulesWithAssets; Min = 2; Suspected = 'the per-module walk collapsed into one module' })
# the direction logic gets its own guards: if the toml reader silently found nothing, every
# legal cross-module reference would be misreported as illegal.  That failure must be loud.
$tomlFound = 0
$localDeps = 0
foreach ($row in $depGraph.Rows) {
    if ($row.Found) { $tomlFound++ }
    $localDeps = $localDeps + @($row.DirectLocal).Count
}
[void]$guards.Add(@{ Label = 'modules whose neoforge.mods.toml was read'; Actual = $tomlFound; Min = 3; Suspected = 'the dependency templates were not found; the direction logic would call every cross-module reference illegal' })
[void]$guards.Add(@{ Label = 'declared required dependencies on a local module'; Actual = $localDeps; Min = 2; Suspected = 'cews/transmutation must each declare createoreexpansion as required; if this is 0 the required-modId parse is broken' })
# asset JSON parse failures and upper-case asset paths are not "at least N" patterns; they
# are absolute requirements and are folded in below.

$guardProblems = New-Object System.Collections.Generic.List[string]
foreach ($g in $guards) {
    $p = Get-GuardProblem -Actual $g.Actual -Minimum $g.Min -Label $g.Label -Suspected $g.Suspected
    if ($null -ne $p) { [void]$guardProblems.Add(($g.Label + ': ' + $p)) }
}
$parseOk = ($parseFailures.Count -eq 0)
$upperOk = ($upperNames.Count -eq 0)
$guardOk = ($guardProblems.Count -eq 0) -and $parseOk -and $upperOk
Write-Check $guardOk 'A0 the scanner is live in every category (anti-vacuity)' `
    $(if ($guardOk) {
        ('guards=' + $guards.Count + ' ; ' + (($guards | ForEach-Object { $_.Label + '=' + $_.Actual }) -join ', '))
    } else {
        (($guardProblems -join ' ;; ') + $(if (-not $parseOk) { ' ;; parse failures=' + $parseFailures.Count } else { '' }) + $(if (-not $upperOk) { ' ;; asset paths with an upper-case letter=' + $upperNames.Count + ' :: ' + (($upperNames | Select-Object -First 5) -join ', ') } else { '' }))
    })
Write-Host ''

# ===========================================================================
# 13. A6 jar dimension (report only)
# ===========================================================================

Write-Host '[I] A6 jar dimension (report only, never fails)'
$zipReady = $true
try { Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction Stop } catch { $zipReady = $false }

$JarDirs = [ordered]@{
    'root'          = 'build/libs'
    'core'          = 'core/build/libs'
    'coe'           = 'coe/build/libs'
    'cews'          = 'cews/build/libs'
    'transmutation' = 'transmutation/build/libs'
}

if ($SkipJars) {
    Write-Sub 'skipped (-SkipJars).'
} elseif (-not $zipReady) {
    Write-Sub 'System.IO.Compression.FileSystem is unavailable in this host; jar comparison skipped.'
} elseif (-not (Test-Path -LiteralPath (Join-Path $RepoRoot 'build/libs'))) {
    Write-Sub 'build/libs does not exist (nothing built yet); jar comparison skipped.'
} else {
    $jarUnits = New-Object System.Collections.Generic.List[object]
    $jarLabels = @{}
    foreach ($m in $JarDirs.Keys) {
        $d = Join-Path $RepoRoot $JarDirs[$m]
        if (-not (Test-Path -LiteralPath $d)) { continue }
        $jars = @(Get-ChildItem -LiteralPath $d -File -Filter '*.jar' | Where-Object { $_.Name -notlike '*-sources*' -and $_.Name -notlike '*-javadoc*' } | Sort-Object Name)
        foreach ($j in $jars) {
            $zip = $null
            try {
                $zip = [System.IO.Compression.ZipFile]::OpenRead($j.FullName)
                $label = $m
                if ($jars.Count -gt 1) { $label = $m + '/' + $j.Name }
                [void]$jarUnits.Add((New-JarUnit $label $j.FullName $zip))
                $jarLabels[$label] = @{ Module = $m; Jar = $j; }
            } catch {
                Write-Sub ('jar unreadable: ' + $j.FullName + ' :: ' + $_.Exception.Message)
            } finally {
                if ($null -ne $zip) { $zip.Dispose() }
            }
        }
    }
    if ($jarUnits.Count -eq 0) {
        Write-Sub 'no jars found under the module build/libs directories.'
    } else {
        $jarArray = $jarUnits.ToArray()
        $jarIndex = New-AssetIndex $jarArray
        # newest asset mtime per module in the WORKING TREE: the reference point for
        # "is this jar older than the resources it is supposed to contain".
        $treeNewest = @{}
        foreach ($m in $JarDirs.Keys) {
            $newest = $null
            foreach ($rr in $ResourceRoots) {
                $base = Join-Path (Join-Path $RepoRoot $ModuleTable[$m]) $rr
                $assets = Join-Path $base ('assets\' + $Namespace)
                if (-not (Test-Path -LiteralPath $assets)) { continue }
                foreach ($f in (Get-ChildItem -LiteralPath $assets -Recurse -File)) {
                    if (($null -eq $newest) -or ($f.LastWriteTime -gt $newest)) { $newest = $f.LastWriteTime }
                }
            }
            $treeNewest[$m] = $newest
        }
        # dependency direction of each jar, read from the jar's own expanded mods.toml
        $jarAllowed = @{}
        foreach ($ju in $jarArray) {
            $decl = $null
            $zip2 = $null
            try {
                $zip2 = [System.IO.Compression.ZipFile]::OpenRead($jarLabels[$ju.Name].Jar.FullName)
                $decl = Get-JarDeclaredModId $zip2
                $tomlText = $null
                foreach ($e in $zip2.Entries) {
                    if ($e.FullName -cne $TomlJarEntry) { continue }
                    $sr = New-Object System.IO.StreamReader($e.Open(), [System.Text.Encoding]::UTF8)
                    try { $tomlText = $sr.ReadToEnd() } finally { $sr.Dispose() }
                }
            } catch {
                $tomlText = $null
            } finally {
                if ($null -ne $zip2) { $zip2.Dispose() }
            }
            $modIdToModule = @{}
            foreach ($mm in $ModuleModIds.Keys) { if ($ModuleModIds[$mm] -ne '') { $modIdToModule[$ModuleModIds[$mm]] = $mm } }
            $set = New-Object System.Collections.Generic.HashSet[string]
            [void]$set.Add($ju.Name)
            if ($null -ne $tomlText) {
                foreach ($b in @(Get-TomlDependencyBlocks $tomlText)) {
                    if ($b.Type -cne 'required') { continue }
                    if ($modIdToModule.ContainsKey($b.ModId)) { [void]$set.Add($modIdToModule[$b.ModId]) }
                }
            }
            $jarAllowed[$ju.Name] = $set
            $jarLabels[$ju.Name].DeclaredModId = $decl
        }
        Write-Sub ('jars scanned: ' + $jarArray.Count + ' (each jar is treated as one self-contained module)')
        foreach ($ju in $jarArray) {
            $nBs = 0; $nIt = 0; $nMd = 0; $nTx = 0
            foreach ($rec in $ju.Files) {
                if ($rec.AssetKind -eq 'blockstate') { $nBs++ }
                elseif ($rec.AssetKind -eq 'model') { $nMd++; if ($rec.AssetRel.StartsWith('models/item/')) { $nIt++ } }
                elseif ($rec.AssetKind -eq 'texture') { $nTx++ }
            }
            $m = $jarLabels[$ju.Name].Module
            $mtime = $jarLabels[$ju.Name].Jar.LastWriteTime
            $treeT = $treeNewest[$m]
            $verdict = 'NO TREE ASSETS'
            if ($null -ne $treeT) {
                if ($mtime -lt $treeT) { $verdict = 'STALE BUILD (jar older than the newest tree asset)' } else { $verdict = 'FRESH (jar newer than every tree asset)' }
            }
            $declText = $jarLabels[$ju.Name].DeclaredModId
            if ($null -eq $declText) { $declText = '(no [[mods]] block)' }
            if ($declText -eq '') { $declText = '(no [[mods]] block)' }
            $tableId = $ModuleModIds[$m]
            $idVerdict = 'MATCHES the module table'
            if ($tableId -eq '') {
                # core is a JarJar GAME LIBRARY: it must NOT declare a mod, so absence is the match
                if ($declText -ne '(no [[mods]] block)') { $idVerdict = 'MISMATCH vs the module table (expected no [[mods]] block)' }
                else { $idVerdict = 'MATCHES the module table (no mod by design)' }
            } elseif ($declText -cne $tableId) {
                $idVerdict = 'MISMATCH vs the module table'
            }
            Write-Sub (('  {0,-20} blockstates={1,-4} item-anchors={2,-4} models={3,-4} textures={4,-4} modId={5,-22} ({6})' -f $ju.Name, $nBs, $nIt, $nMd, $nTx, $declText, $idVerdict))
            Write-Sub (('      jar mtime={0:yyyy-MM-dd HH:mm:ss} newest tree asset={1} -> {2}' -f $mtime, $(if ($null -eq $treeT) { 'n/a' } else { ('{0:yyyy-MM-dd HH:mm:ss}' -f $treeT) }), $verdict))
        }
        $jarScan = Invoke-LocalityScan $jarArray $jarIndex $jarAllowed
        $jc = $jarScan.Counts
        Write-Host ''
        Write-Sub ('jar references: total=' + $jc.RefsTotal + ' in-namespace=' + $jc.RefsInScope + ' local=' + $jc.Local + ' legal-cross=' + $jc.LegalCross + ' illegal-direction=' + $jc.IllegalCross + ' dangling=' + $jc.Dangling)
        Write-Sub 'jar timeline note: a jar is a snapshot of the build that produced it.  A reference that fails only inside a STALE BUILD jar is a TIMING fact (the source tree has already moved on), not a product verdict.  Only a FRESH jar may be read as the shipped truth.'
        Write-Host ''
        Write-Sub 'source tree vs jar inventory (per module):'
        $treeUnits = @{}
        foreach ($u in $units) { $treeUnits[$u.Name] = $u }
        foreach ($ju in $jarArray) {
            $m = $jarLabels[$ju.Name].Module
            if (-not $treeUnits.ContainsKey($m)) { continue }
            $tu = $treeUnits[$m]
            $tSet = @{}
            foreach ($rec in $tu.Files) { if ($rec.AssetKind -ne '') { $tSet[$rec.AssetRel] = $true } }
            $jSet = @{}
            foreach ($rec in $ju.Files) { if ($rec.AssetKind -ne '') { $jSet[$rec.AssetRel] = $true } }
            $onlyTree = @($tSet.Keys | Where-Object { -not $jSet.ContainsKey($_) })
            $onlyJar = @($jSet.Keys | Where-Object { -not $tSet.ContainsKey($_) })
            if (($onlyTree.Count -eq 0) -and ($onlyJar.Count -eq 0)) {
                Write-Sub ('  {0,-20} IN SYNC with the working tree ({1} asset file(s))' -f $ju.Name, $jSet.Count)
            } else {
                Write-Sub ('  {0,-20} DIFF (not a verdict: the jar predates or postdates the tree) -- only-in-tree={1} only-in-jar={2}' -f $ju.Name, $onlyTree.Count, $onlyJar.Count)
                foreach ($p in ($onlyTree | Select-Object -First 6)) { Write-Sub ('      only in tree: ' + $p) }
                foreach ($p in ($onlyJar | Select-Object -First 6)) { Write-Sub ('      only in jar : ' + $p) }
            }
        }
        Write-Host ''
        # the published-shape answer to "which jar needs a file that another jar carries"
        $jarStrand = @{}
        foreach ($ju in $jarArray) {
            $vis = Get-ClosureVisit $ju.Name $jarArray $jarIndex
            foreach ($p in $vis.Models.Keys) {
                $ownerMod = $vis.Models[$p]
                if ($ownerMod -eq $ju.Name) { continue }
                $k = $ju.Name + '|' + $ownerMod + '|model'
                if (-not $jarStrand.ContainsKey($k)) { $jarStrand[$k] = New-Object System.Collections.Generic.HashSet[string] }
                [void]$jarStrand[$k].Add($p)
            }
            foreach ($p in $vis.Textures.Keys) {
                $ownerMod = $vis.Textures[$p]
                if ($ownerMod -eq $ju.Name) { continue }
                $k = $ju.Name + '|' + $ownerMod + '|texture'
                if (-not $jarStrand.ContainsKey($k)) { $jarStrand[$k] = New-Object System.Collections.Generic.HashSet[string] }
                [void]$jarStrand[$k].Add($p)
            }
        }
        if ($jarStrand.Count -eq 0) {
            Write-Sub 'jar closure: no jar needs an asset that lives in another jar.'
        } else {
            Write-Sub 'jar closure (published shape):'
            foreach ($k in ($jarStrand.Keys | Sort-Object)) {
                $parts = $k.Split('|')
                Write-Sub (('  {0,-20} needs {1,-8} from {2,-20} distinct files={3}' -f $parts[0], $parts[2], $parts[1], $jarStrand[$k].Count))
            }
        }
        Write-Host ''
        $jarProblems = @($jarScan.Problems)
        $jUniq = @{}
        foreach ($p in $jarProblems) { $jUniq[$p.Module + '|' + $p.Rel + '|' + $p.Kind + '|' + $p.Target] = $p }
        if ($jUniq.Count -eq 0) {
            Write-Sub 'jar attribution: illegal=0 dangling=0 (the published artefacts are internally attributed).'
        } else {
            Write-Sub ('jar attribution: illegal/dangling distinct reference(s)=' + $jUniq.Count + ' -- REPORT ONLY, NEVER SCORED.')
            Write-Sub '  Read this together with the STALE/FRESH verdict above: a reference that fails only inside a'
            Write-Sub '  STALE BUILD jar is a timing fact.  The listed entries are the published shape of the moment'
            Write-Sub '  the jar was built, not a verdict about the current working tree.'
            $shown = 0
            foreach ($k in ($jUniq.Keys | Sort-Object)) {
                if ($shown -ge 60) { Write-Sub ('  ... and ' + ($jUniq.Count - 60) + ' more'); break }
                $p = $jUniq[$k]
                Write-Sub (('  {0} | {1} | {2}={3} | lives in: {4}' -f $p.Module, $p.Rel, $p.Kind, $p.Target, $p.Home))
                $shown++
            }
        }
    }
}
Write-Host ''

# ===========================================================================
# 14. coverage boundary
# ===========================================================================

Write-Host '[J] coverage boundary (what this gate deliberately does not see)'
$loaderModels = 0
foreach ($u in $units) {
    foreach ($rec in $u.Files) {
        if ($rec.AssetKind -eq 'model' -and $rec.ParseError -eq '') {
            if (Test-IsObject (Get-ObjectValue $rec.Doc 'loader')) { $loaderModels++ }
        }
    }
}
Write-Sub ('loader (neoforge:obj) models in scope: ' + $loaderModels + '  -- an obj model points at a .obj file via a top-level "model" field that is NOT a resource id; such a file would be reported as uncovered, not silently ignored.')
Write-Sub 'not judged: third-party namespaces, namespace-less ids (they are minecraft:), data/** and lang/**, particles/ atlases/ font/ (absent from this tree).'
Write-Sub 'covered: blockstates/*.json, models/**/*.json (parent, textures, overrides[].model), textures/**/*.png existence, and the declared dependency direction of each of them.'
Write-Host ''

# ===========================================================================
# 15. summary
# ===========================================================================

Write-Host '=========================================================================='
Write-Host ('checks run: ' + $script:checkCount + ', failed: ' + $script:failures.Count)
if ($script:failures.Count -eq 0) {
    Write-Host 'OK: every module carries the assets its own files reference, or declares the module that does.'
    exit 0
}
Write-Host 'FAILED assertions:'
foreach ($f in $script:failures) { Write-Host ('  - ' + $f) }
Write-Host ''
Write-Host 'Reminder: an A1 ILLEGAL failure means "install this module alone and these assets are gone",'
Write-Host 'because the owning module is not in this module''s declared required set.  The fix is to MOVE'
Write-Host 'the named files into the referencing module -- or to declare the dependency, which is only'
Write-Host 'correct when the owning module really is mandatory.  A copy is not a fix (A2 turns red).'
exit 1
