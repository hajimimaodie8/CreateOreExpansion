# check-package-heritage.ps1 -- package-name heritage assertion for createoreexpansion.
#
# WHY THIS EXISTS
#   The module split (P3w/P3y/P3z) renamed packages whose files moved into a new Gradle
#   module.  It did that because tools\check-layering.ps1 decides a FILE's layer from its
#   PATH, so a package name was bent to satisfy the checker -- even though a package name
#   does not decide which Gradle module a file lives in, and a package that lies about its
#   layer is exactly the mistake this repository already paid for once (content/ore/).
#   P12 restored every package name that could be restored and rewrote the layer rules
#   instead.  This script is the permanent guard that keeps the two sets apart:
#
#     FORCED     the pre-split package name is now claimed by ANOTHER module, so the class
#                cannot go home without breaking the JPMS rule "one package belongs to at
#                most one mod file".  Nothing to do; the rename is legitimate.
#     NON-FORCED the pre-split package name is free (or already claimed by this class's own
#                module) and the class still lives somewhere else -> it could go home
#                today.  Every entry of this list is a rename that exists only to please a
#                path-based rule, so the script FAILS (exit 1) when the list is non-empty.
#
#   So the criterion is mechanical and needs no whitelist, no judgment call and no
#   per-class exception: "is the old name still mine to take?"
#
# BASELINE
#   fbf33cdf~1 -- the last commit with everything inside the root project, i.e. the
#   package layout before the split renamed anything.  Override with -Base <rev>.
#
# PAIRING
#   baseline file -> current file is established in this order:
#     1. same package-relative path under any of the five roots (package unchanged);
#     2. git's own rename detection (`git diff -M --name-status <base>`);
#     3. the class's simple name, when it is unique in BOTH trees.
#   Anything still unpaired is reported as UNPAIRED and never silently dropped; the
#   accounting line at the end proves that every baseline class was classified.
#
# USAGE
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-package-heritage.ps1
#   Exit code: 0 = no non-forced rename, 1 = at least one class could go home today.
#
# NOTE: this file is deliberately pure ASCII.  Windows PowerShell 5.1 reads a BOM-less
# .ps1 as ANSI, which mangles non-ASCII text and can even swallow quotes mid-script.
# Read-only: it never writes anything.

param(
    [string]$Base = 'fbf33cdf~1'
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$prefix   = 'com/hjmmd_8/createoreexpansion/'

# The same five source roots the other layer tools scan.  Core is a root like any other:
# its files are the shared library and its packages count for the overlap criterion too.
$roots = [ordered]@{
    'root'          = 'src/main/java'
    'core'          = 'core/src/main/java'
    'coe'           = 'coe/src/main/java'
    'cews'          = 'cews/src/main/java'
    'transmutation' = 'transmutation/src/main/java'
}

function Split-JarRel {
    param([string]$jarRel)
    if (-not $jarRel.EndsWith('.java')) { return $null }
    if (-not $jarRel.StartsWith($prefix)) { return $null }
    $inner = $jarRel.Substring($prefix.Length)
    $inner = $inner.Substring(0, $inner.Length - 5)
    $parts = $inner -split '/'
    $cls = $parts[-1]
    $pkg = 'com.hjmmd_8.createoreexpansion'
    if ($parts.Length -gt 1) { $pkg = $pkg + '.' + (($parts[0..($parts.Length - 2)]) -join '.') }
    return [pscustomobject]@{ Pkg = $pkg; Cls = $cls }
}

Push-Location $repoRoot
try {
    # `git` prints "LF will be replaced by CRLF" for every rewritten file (this repo has
    # core.autocrlf=true with LF blobs).  That advisory must not reach the report, and
    # under $ErrorActionPreference='Stop' PowerShell turns a native stderr line into a
    # TERMINATING error, so the two git calls are made with the preference relaxed and
    # stderr dropped.  A real git failure still aborts via $LASTEXITCODE.
    $eap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'

    # ---- baseline: package-relative path -> package -------------------------------
    $basePkg = @{}
    $baseCls = @{}
    $lsOut = & git ls-tree -r --name-only $Base -- 'src/main/java' 2>$null
    if ($LASTEXITCODE -ne 0) { throw "git ls-tree failed for $Base" }
    foreach ($line in $lsOut) {
        $l = $line.Trim()
        if (-not $l.StartsWith('src/main/java/')) { continue }
        $jarRel = $l.Substring('src/main/java/'.Length)
        $s = Split-JarRel $jarRel
        if ($null -eq $s) { continue }
        $basePkg[$jarRel] = $s.Pkg
        if (-not $baseCls.ContainsKey($s.Cls)) { $baseCls[$s.Cls] = New-Object System.Collections.Generic.List[string] }
        $baseCls[$s.Cls].Add($jarRel)
    }
    if ($basePkg.Count -eq 0) { throw "no baseline java files found at $Base" }

    # ---- current: package-relative path -> module / package -----------------------
    $curMod = @{}
    $curPkg = @{}
    $curCls = @{}
    foreach ($rootName in $roots.Keys) {
        $rootPath = Join-Path $repoRoot $roots[$rootName]
        if (-not (Test-Path -LiteralPath $rootPath)) { throw "source root not found: $rootPath" }
        $rootAbs = (Resolve-Path -LiteralPath $rootPath).Path
        foreach ($f in (Get-ChildItem -LiteralPath $rootAbs -Recurse -File -Filter *.java)) {
            $jarRel = $f.FullName.Substring($rootAbs.Length).TrimStart('\', '/') -replace '\\', '/'
            if (-not $jarRel.StartsWith($prefix)) { continue }
            $s = Split-JarRel $jarRel
            if ($null -eq $s) { continue }
            if ($curMod.ContainsKey($jarRel)) { throw "same class path in two roots: $jarRel" }
            $curMod[$jarRel] = $rootName
            $curPkg[$jarRel] = $s.Pkg
            if (-not $curCls.ContainsKey($s.Cls)) { $curCls[$s.Cls] = New-Object System.Collections.Generic.List[string] }
            $curCls[$s.Cls].Add($jarRel)
        }
    }

    # which modules claim each package, right now
    $pkgOwners = @{}
    foreach ($jarRel in $curPkg.Keys) {
        $p = $curPkg[$jarRel]
        if (-not $pkgOwners.ContainsKey($p)) { $pkgOwners[$p] = New-Object 'System.Collections.Generic.HashSet[string]' }
        [void]$pkgOwners[$p].Add($curMod[$jarRel])
    }

    # ---- git rename map + deletions ----------------------------------------------
    $renamed = @{}
    $deleted = New-Object 'System.Collections.Generic.HashSet[string]'
    $pathSpec = @()
    foreach ($k in $roots.Keys) { $pathSpec += $roots[$k] }
    # 2>$null only silences git's "LF will be replaced by CRLF" advisory (see above).
    $diffOut = & git diff -M --name-status $Base -- $pathSpec 2>$null
    if ($LASTEXITCODE -ne 0) { throw "git diff failed for $Base" }
    $ErrorActionPreference = $eap
    foreach ($line in $diffOut) {
        $l = $line.TrimEnd()
        if ($l -notmatch '^([A-Z])\d*\t(.+)$') { continue }
        $status = $Matches[1]
        $rest = $Matches[2] -split "`t"
        if ($status -eq 'R' -and $rest.Count -ge 2) {
            $old = $rest[0]; $new = $rest[1]
            if ($old.StartsWith('src/main/java/')) {
                # git reports the new side as a REPO path (coe/src/main/java/...), while the
                # current index is keyed by the PACKAGE-relative path.  Keeping the root
                # prefix here made every git-detected rename fail its lookup and fall into
                # UNPAIRED -- 26 classes were silently mis-filed before this was fixed.
                $newRel = $new
                $i = $newRel.IndexOf('com/hjmmd_8/createoreexpansion/')
                if ($i -ge 0) { $newRel = $newRel.Substring($i) }
                $renamed[$old.Substring('src/main/java/'.Length)] = $newRel
            }
        }
        elseif ($status -eq 'D' -and $rest.Count -ge 1) {
            $old = $rest[0]
            if ($old.StartsWith('src/main/java/')) {
                [void]$deleted.Add($old.Substring('src/main/java/'.Length))
            }
        }
    }

    # ---- classify every baseline class --------------------------------------------
    $forced    = New-Object System.Collections.ArrayList
    $nonForced = New-Object System.Collections.ArrayList
    $gone      = New-Object System.Collections.ArrayList
    $unpaired  = New-Object System.Collections.ArrayList
    $modOnly   = 0
    $samePkg   = 0

    foreach ($jarRel in ($basePkg.Keys | Sort-Object)) {
        $bPkg = $basePkg[$jarRel]
        $now  = $null
        $how  = ''
        if ($curPkg.ContainsKey($jarRel)) { $now = $jarRel; $how = 'same path' }
        elseif ($renamed.ContainsKey($jarRel)) { $now = $renamed[$jarRel]; $how = 'git rename' }
        else {
            $cls = (Split-JarRel $jarRel).Cls
            if ($baseCls[$cls].Count -eq 1 -and $curCls.ContainsKey($cls) -and $curCls[$cls].Count -eq 1) {
                $now = $curCls[$cls][0]; $how = 'unique simple name'
            }
        }
        if ($null -eq $now -or -not $curPkg.ContainsKey($now)) {
            if ($deleted.Contains($jarRel) -or -not $renamed.ContainsKey($jarRel)) {
                [void]$gone.Add($jarRel)
            } else {
                [void]$unpaired.Add($jarRel)
            }
            continue
        }
        $m = $curMod[$now]
        $p = $curPkg[$now]
        if ($p -eq $bPkg) {
            # the package-relative path is the same, so this class only changed GRADLE
            # MODULE (baseline is the root project); harmless, nothing to restore.
            $modOnly++
            continue
        }
        $others = @()
        if ($pkgOwners.ContainsKey($bPkg)) {
            $others = @($pkgOwners[$bPkg] | Where-Object { $_ -ne $m } | Sort-Object)
        }
        if ($others.Count -eq 0) {
            [void]$nonForced.Add([pscustomobject]@{ Cls = (Split-JarRel $jarRel).Cls; From = $bPkg; To = $p; Module = $m; How = $how })
        } else {
            [void]$forced.Add([pscustomobject]@{ Cls = (Split-JarRel $jarRel).Cls; From = $bPkg; To = $p; Module = $m; Owners = ($others -join ',') })
        }
    }

    # ---- report ---------------------------------------------------------------------
    Write-Host ('package heritage: baseline ' + $Base + ' vs the working tree')
    Write-Host ('  baseline classes = {0} ; current classes = {1}' -f $basePkg.Count, $curPkg.Count)
    Write-Host ''
    Write-Host ('FORCED renames ({0}) -- the pre-split package is claimed by another module:' -f $forced.Count)
    foreach ($f in ($forced | Sort-Object Cls)) {
        Write-Host ('  {0,-38} {1}' -f $f.Cls, $f.From)
        Write-Host ('      now {0}  [{1}]  -- old package held by: {2}' -f $f.To, $f.Module, $f.Owners)
    }
    Write-Host ''
    Write-Host ('NON-FORCED renames ({0}) -- the pre-split package is free; these must go home:' -f $nonForced.Count)
    foreach ($f in ($nonForced | Sort-Object Cls)) {
        Write-Host ('  {0,-38} {1}  ->  now {2}  [{3}]  (paired by {4})' -f $f.Cls, $f.From, $f.To, $f.Module, $f.How)
    }
    Write-Host ''
    Write-Host ('  package unchanged            : {0}' -f $samePkg)
    Write-Host ('  module changed only          : {0}   (harmless: the package name is the pre-split one)' -f $modOnly)
    Write-Host ('  class gone from the baseline : {0}' -f $gone.Count)
    foreach ($g in ($gone | Sort-Object)) { Write-Host ('      GONE {0}' -f $g) }
    Write-Host ('  unpaired (needs a human look): {0}' -f $unpaired.Count)
    foreach ($u in ($unpaired | Sort-Object)) { Write-Host ('      UNPAIRED {0}' -f $u) }
    Write-Host ''
    $accounted = $samePkg + $modOnly + $forced.Count + $nonForced.Count + $gone.Count + $unpaired.Count
    Write-Host ('accounting: {0} of {1} baseline classes classified' -f $accounted, $basePkg.Count)
    if ($accounted -ne $basePkg.Count) {
        Write-Host 'ACCOUNTING MISMATCH -- the classification above is incomplete.  Fix the script.'
        exit 1
    }

    if ($nonForced.Count -eq 0) {
        Write-Host ''
        Write-Host 'OK: every package rename that survives is forced by the one-package-one-module rule.'
        exit 0
    }
    Write-Host ''
    Write-Host ('VIOLATION: {0} class(es) could still go back to their pre-split package.' -f $nonForced.Count)
    Write-Host 'FIX: either move them home, or write the layer rule that names their pre-split path'
    Write-Host '     (tools\check-layering.ps1 + tools\layer-usage.ps1, Get-FileLayer byte-identical).'
    exit 1
}
finally {
    Pop-Location
}
