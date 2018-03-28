Param (
  [String] $Connection,
  [String[]] $Objects,
  [String[]] $Folders,
  [Int] $RuleSet,
  [String] $ReportName,
  [String] $ReportFolder,
  [Switch] $HTML,
  [Switch] $JSON,
  [Switch] $XLS,
  [Switch] $XML
)

# Decode Oracle connection string
$DecodedConnection = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Connection))

# Decode database objects formatted as "type.owner.name"
$DecodedObjects = @()
if ($Objects) {
  ForEach($Object in $Objects) {
    $Type, $Owner, $Name = $Object.Split('.')

    $DecodedType = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Type))
    $DecodedOwner = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Owner))
    $DecodedName = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Name))

    $DecodedObject = New-Object PSObject
    $DecodedObject | Add-Member NoteProperty Type $DecodedType
    $DecodedObject | Add-Member NoteProperty Owner $DecodedOwner
    $DecodedObject | Add-Member NoteProperty Name $DecodedName

    $DecodedObjects += $DecodedObject
  }
}

# Decode database object folders
$DecodedFolders = @()
if ($Folders) {
  ForEach($Folder in $Folders) {
    $Path, $Filter, $Recurse = $Folder.Split('.')

    $DecodedPath = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Path))
    $DecodedFilter = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Filter))
    $DecodedRecurse = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Recurse))

    # Convert from string to boolean
    $DecodedRecurse = [System.Convert]::ToBoolean($DecodedRecurse)

    $DecodedFolder = New-Object PSObject
    $DecodedFolder | Add-Member NoteProperty Path $DecodedPath
    $DecodedFolder | Add-Member NoteProperty Filter $DecodedFilter
    $DecodedFolder | Add-Member NoteProperty Recurse $DecodedRecurse

    $DecodedFolders += $DecodedFolder
  }
}

# Decode report arguments
$DecodedReportName = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($ReportName))
$DecodedReportFolder = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($ReportFolder))

# Start TDT
$TDT = New-Object -ComObject 'Toad.ToadAutoObject'
try {

  # Create the output path if it doesn't exist
  if ($DecodedReportFolder) {
    New-Item -itemType Directory -force -path $DecodedReportFolder | Out-Null
  }

  # Set CA parameters
  $TDT.CodeAnalysis.Connection = $TDT.Connections.NewConnection($DecodedConnection)

  $TDT.CodeAnalysis.RuleSet = $RuleSet

  $TDT.CodeAnalysis.ReportName = $DecodedReportName
  $TDT.CodeAnalysis.OutputFolder = $DecodedReportFolder
  $TDT.CodeAnalysis.ReportFormats.IncludeHTML = $HTML
  $TDT.CodeAnalysis.ReportFormats.IncludeJSON = $JSON
  $TDT.CodeAnalysis.ReportFormats.IncludeXLS = $XLS
  $TDT.CodeAnalysis.ReportFormats.IncludeXML = $XML

  # Set files to analyze
  ForEach($DecodedFolder in $DecodedFolders) {
    if ($DecodedFolder.Recurse) {
      Get-ChildItem $DecodedFolder.Path -filter $DecodedFolder.Filter -recurse | % {
        if (!$_.PSIsContainer) {
          $TDT.CodeAnalysis.Files.Add($_.FullName)
        }
      }
    } else {
      Get-ChildItem $DecodedFolder.Path -filter $DecodedFolder.Filter | % {
        if (!$_.PSIsContainer) {
          $TDT.CodeAnalysis.Files.Add($_.FullName)
        }
      }
    }
  }

  # Set database object info to analyze
  ForEach($DecodedObject in $DecodedObjects) {
    $DBObject = $TDT.CodeAnalysis.DBObjects.Add()
    $DBObject.ObjectName  = $DecodedObject.Name
    $DBObject.ObjectOwner = $DecodedObject.Owner
    $DBObject.ObjectType  = $DecodedObject.Type
  }

  # Execute Code Analysis
  $TDT.CodeAnalysis.Execute()

} finally {
  $TDT.Quit()
}