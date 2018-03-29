#Requires -Version 2

Param (
  [String] $Connection,
  [Int] $MaxRows,
  [String] $InputFile,
  [String] $OutputFile
)

# Decode Oracle connection string
$DecodedConnection = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Connection))

# Decode input file
$DecodedInputFile = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($InputFile))

# Decode output file
$DecodedOutputFile = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($OutputFile))

# Start TDT
$TDT = New-Object -ComObject 'Toad.ToadAutoObject'
try {
  # Set script parameters
  $Script = $TDT.Scripts.Add()
  $Script.Connection = $TDT.Connections.NewConnection($DecodedConnection)
  $Script.IncludeOutput = $TRUE
  $Script.MaxRows = $MaxRows
  $Script.InputFile = $DecodedInputFile
  $Script.OutputFile = $DecodedOutputFile

  # Execute script
  $Script.Execute()

  # Display the script execution time
  Write-Output ("Script execution time: {0}" -f $Script.ExecutionTime)

  if ($Script.ErrorCount -gt 0) {
    Write-Output ("Script errors: {0}" -f $Script.ErrorCount)
    Write-Output 'FAILURE'
  }
} catch {
  Write-Output $_.Exception.Message
  Write-Output 'FAILURE'
} finally {
  $TDT.Quit()
}
