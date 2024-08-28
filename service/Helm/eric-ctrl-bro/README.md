# Chart Details

Ericsson Backup and Restore Orchestrator for microservices.

## Volumes

The chart supports mounting an arbitrary number of volumes.
Each volume will be mounted on the Backup And Restore Orchestrator.
Volumes and their mount locations can be defined in the *volumes* and *volumeMounts* respectively.
 
## Volume

**Note:** Volume and Volume Claims are provided in this chart for the Orchestrator container. If the user wishes to add more, then guidelines can be found below.

 
The example below shows how to mount multiple volumes:
```
volumes and volume mounts, if more are required they can be added here -
volumes: |
  - name: eric-ctrl-bro-data
   persistentVolumeClaim:
      claimName: eric-ctrl-bro-data
volumeMounts: |
  - name: eric-ctrl-bro-data
    mountPath: "/data"
    subPath: backups
```
