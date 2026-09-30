# Compare each file changed by the COE-embed series against upstream master:
# report line counts and any PUBLIC STATIC method names present upstream but missing now.
# Pure ASCII. Usage: skiller-api-diff.ps1
param([string]$Repo = 'E:\mc\mcmod\_ref\Skiller')

Push-Location $Repo
$base = 'origin/master'
$files = git diff --name-only "$base..HEAD" 2>&1 | Where-Object { $_ -like '*.java' }
Write-Output "changed vs $base : $($files.Count) files"
Write-Output ''

foreach ($f in $files) {
    $upPath = Join-Path $env:TEMP ('up_' + ($f -replace '[/\\]', '_'))
    git show "$base`:$f" > $upPath 2>$null
    if (-not (Test-Path $upPath)) { continue }
    $curPath = Join-Path $Repo $f
    if (-not (Test-Path $curPath)) { Write-Output "MISSING IN WORKTREE: $f"; continue }
    $up = Get-Content $upPath -Encoding UTF8
    $cur = Get-Content $curPath -Encoding UTF8

    # public static method signatures (name only)
    $sigRe = '^\s*public\s+static\s+[\w<>,\[\]\.\?\s]+?\s+(\w+)\s*\('
    $upMethods = @()
    foreach ($l in $up) { if ($l -match $sigRe) { $upMethods += $Matches[1] } }
    $curMethods = @()
    foreach ($l in $cur) { if ($l -match $sigRe) { $curMethods += $Matches[1] } }
    $missing = $upMethods | Where-Object { $_ -notin $curMethods } | Sort-Object -Unique

    # @SubscribeEvent methods
    $upSub = ($up | Select-String -Pattern '@SubscribeEvent').Count
    $curSub = ($cur | Select-String -Pattern '@SubscribeEvent').Count

    $flag = ''
    if ($missing.Count -gt 0) { $flag += ' MISSING-METHODS' }
    if ($curSub -lt $upSub) { $flag += ' FEWER-SUBSCRIBERS' }
    if ($flag -ne '') {
        Write-Output ("{0,-70} up={1,4} cur={2,4} sub={3}/{4} {5}" -f $f, $up.Count, $cur.Count, $curSub, $upSub, $flag)
        if ($missing.Count -gt 0) { Write-Output ("    upstream-only public static: " + ($missing -join ', ')) }
    }
}
Pop-Location
