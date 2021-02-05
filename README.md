Updater
=======
Simple application to download and apply OTA packages.


Server requirements
-------------------
The app sends `GET` requests to the URL defined by the `updater_server_url`
resource (or the `dot.updater.uri` system property) and expects as response
a JSON with the following structure:
```json
{
  "response":[
    {
      "datetime":1612459475,
      "filename":"update.zip",
      "id":"817af9b5b8eab6151c22456634c24dfe",
      "romtype":"OFFICIAL",
      "size":1666374431,
      "url":"https://url/update.zip",
      "version":"v5.0.0",
      "changelog":[
        {
          "miscTitle":"",
          "miscSummary":"", 
          "settingsTitle":"",
          "settingsSummary":"",
          "securityTitle":"",
          "securitySummary":"",
          "systemTitle":"",
          "systemSummary":""
        }
      ]
    }
  ]
}
```

The `datetime` attribute is the build date expressed as UNIX timestamp.  
The `filename` attribute is the name of the file to be downloaded.  
The `id` attribute is a string that uniquely identifies the update.  
The `romtype` attribute is the string to be compared with the `ro.dot.releasetype` property.  
The `size` attribute is the size of the update expressed in bytes.  
The `url` attribute is the URL of the file to be downloaded.  
The `version` attribute is the string to be compared with the `ro.modversion` property.  
The `changelog` contains the title and summary for each category of changes possible yet.

Additional attributes are ignored.
