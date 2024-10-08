# Convert sonarqube's issues search api json to sonar-report.json

{
  "version": "7.8",
  "users": [],
  "issues": [
    .issues[] | {
      "creationDate": .creationDate,
      "isNew": true,
      "status": "OPEN",
      "rule": .rule,
      "severity": .severity,
      "key": .key,
      "component": .component,
      "line": .textRange | .startLine,
      "startLine": .textRange | .startLine,
      "startOffset": .textRange | .startOffset,
      "endLine": .textRange | .endLine,
      "endOffset": .textRange | .endOffset,
      "message": .message
    }
  ],
  "components": [
    .components[] |
      if .qualifier == "TRK" then
        { "key": .key }
      else
        {
          "status": "CHANGED",
          "moduleKey": .key | split(":src")[0],
          "path": .path,
          "key": .key
        }
      end
  ]
}

