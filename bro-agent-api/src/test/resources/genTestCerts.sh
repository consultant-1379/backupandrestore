#!/bin/bash
cd src/test/resources

if [ $1 == generate ]
 then
  openssl genrsa -out ca.key 1024
  openssl req -x509 -new -nodes -key ca.key -subj '/CN=testca' -sha256 -days 1 -out ca.pem

  openssl genrsa -out testca.key 2048
  openssl req -x509 -new -nodes -key testca.key -subj '/CN=CA CERTIFICATE' -sha256 -days 1 -out testca.pem

  openssl genrsa -out broca.pem 2048
  openssl pkcs8 -topk8 -in broca.pem -out broca.key -nocrypt
  openssl req -new -sha256 -key broca.key -subj '/CN=CA INTERMEDIARY CERTIFICATE' -sha256 -days 1 -out broca.csr
  openssl x509 -req -in broca.csr -CA testca.pem -CAkey testca.key -CAcreateserial -out broca.pem

  openssl genrsa -out server1.pem 2048
  openssl pkcs8 -topk8 -in server1.pem -out server1.key -nocrypt
  openssl req -new -sha256 -key server1.key -subj "/CN=*.test.google.com" -out server1.csr -config openssl.conf

  openssl x509 -req -in server1.csr -CA ca.pem -CAkey ca.key -CAcreateserial -out server1.pem -days 1 -sha256 -extensions req_ext -extfile openssl.conf

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

else
 echo "Invalid Parameter please use {generate | remove}"
fi
