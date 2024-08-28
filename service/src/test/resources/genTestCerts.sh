#!/bin/bash
cd src/test/resources

if [ $1 == generate ]
 then
  openssl ecparam -name prime256v1 -genkey -noout -out ca.key
  openssl req -x509 -new -nodes -key ca.key -subj '/CN=ca' -sha256 -days 1 -out ca.pem

  openssl ecparam -name prime256v1 -genkey -noout -out broca.key
  openssl req -x509 -new -nodes -key broca.key -subj '/CN=broca' -sha256 -days 1 -out broca.pem

  mkdir -p cmmserver/ca
  openssl req -x509 -new -nodes -key broca.key -subj '/CN=legacyca' -sha256 -days 1 -out cmmserver/ca/client-cacertbundle.pem
  mkdir -p action-client-cacert
  openssl req -x509 -new -nodes -key broca.key -subj '/CN=actionca' -sha256 -days 1 -out action-client-cacert/client-cacert.pem
  mkdir -p statedata-client-cacert
  openssl req -x509 -new -nodes -key broca.key -subj '/CN=stateca' -sha256 -days 1 -out statedata-client-cacert/client-cacert.pem
  mkdir -p validator-client-cacert
  openssl req -x509 -new -nodes -key broca.key -subj '/CN=validatorca' -sha256 -days 1 -out validator-client-cacert/client-cacert.pem

  openssl ecparam -name prime256v1 -genkey -noout -out server1.key
  openssl req -new -sha256 -key server1.key -subj "/CN=localhost" -out server1.csr

  openssl x509 -req -in server1.csr -CA ca.pem -CAkey ca.key -CAcreateserial -out server1.pem -days 1 -sha256

  keytool -import -alias BRO -file ca.pem -keystore ca.p12 -storetype PKCS12 -storepass broTestPassword -noprompt

  openssl ecparam -name prime256v1 -genkey -noout -out clientprivkey.pem
  openssl pkcs8 -topk8 -in clientprivkey.pem -out clientprivkey.key -nocrypt
  openssl req -new -sha256 -key clientprivkey.key -subj "/CN=CLIENT" -out client.csr

  openssl x509 -req -in client.csr -CA broca.pem -CAkey broca.key -CAcreateserial -out clientcert.pem -days 1 -sha256

  openssl ecparam -name prime256v1 -genkey -noout -out cmmclientprivkey.pem
  openssl req -x509 -new -nodes -key cmmclientprivkey.pem -subj '/CN=eric-cm-mediator-ICCA' -sha256 -days 1 -out cmmclientcert.pem

  openssl ecparam -name prime256v1 -genkey -noout -out invalidPrivateKey.key
  openssl req -x509 -new -nodes -key invalidPrivateKey.key -subj '/CN=invalid' -sha256 -days 1 -out invalidCert.pem

  echo "failed" > invalidCert.pem

  rm -rf invalidPrivateKey.key
  echo "sdaf" > invalidPrivateKey.key
elif [ $1 == remove ]
 then
  rm *.pem *.p12 *.srl *.csr *.key
  rm -rf cmmserver action-client-cacert statedata-client-cacert validator-client-cacert

else
 echo "Invalid Parameter please use {generate | remove}"
fi
