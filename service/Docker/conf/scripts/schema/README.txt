The directories in this folder are below: 

- source/ : contains the source yang files for the BRO implementation of the ericsson-brm model.
            These are:
                    - ericsson-brm.yang [The source EOI brm model. This must not be edited unless approved by the YMF architects]
                    - ericsson-brm-ext-adp.yang [The yang extension module detailing the BRO deviation from the source EOI brm model]


- source/overlay/ : contains an extension file for setting config true and static data true
                          - ericsson-brm-ext-static-data-dev.yang

- preprocessed/ : contains the processed/generated yang files after running the 'Ericsson Yang Utilities' jar against the files in source/.
                  This is necessary to add the required tailf annotations for use with CM Yang Provider.
                  

- preprocessed/overlay/ : contains an extension file for setting config true and static data true and models.json file
                          - ericsson-brm-ext-static-data-dev.yang
                          - models.json with the additional extension file

Model Generation Steps:

If there is a change in any of the model files below, the model archive and json will need to be regenerated.

   - ericsson-brm.yang
   - ericsson-brm-ext-adp.yang
   - ericsson-brm-ext-static-data-dev.yang


Generate the YANG Archive, model json and Yang Library file  by following the instructions: 
  https://eteamspace.internal.ericsson.com/display/ADPHUBPDUCD/ADP+Model+Processing+Example


The Ericsson Yang Utilities releases are currently found here:
   https://eecs-eclipse.sero.wh.rnd.internal.ericsson.com/updates/customer/dx/dropbox/yt-utilities/
   

After the files have been generated:

- Copy the archive to directory 'service/src/main/resources/' to be used by BRO.
- Copy the generated json schema 'ericsson-brm.json' to directory 'service/src/main/resources/' to be used by BRO.
- Copy the Yang library file to 'service/SupportingDocumentation/yang/cpi/eric-ctrl-bro_yang_library.xml'. This will be used by CPI team.

