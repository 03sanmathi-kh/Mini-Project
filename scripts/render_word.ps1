param([Parameter(Mandatory=$true)][string]$InputDocx,[Parameter(Mandatory=$true)][string]$OutputPdf)
$ErrorActionPreference='Stop'
$word=$null
$document=$null
try {
    $word=New-Object -ComObject Word.Application
    $word.Visible=$false
    $word.DisplayAlerts=0
    $document=$word.Documents.Open([System.IO.Path]::GetFullPath($InputDocx),$false,$true)
    $document.ExportAsFixedFormat([System.IO.Path]::GetFullPath($OutputPdf),17)
    Write-Output 'Word PDF render completed.'
} finally {
    if ($null -ne $document) { $document.Close(0) }
    if ($null -ne $word) { $word.Quit() }
}
