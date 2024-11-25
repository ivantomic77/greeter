#!/bin/bash

cd /opt/keycloak/bin/

./kcadm.sh config credentials --server http://localhost:8080 --realm master --user ${KEYCLOAK_ADMIN} --password ${KEYCLOAK_ADMIN_PASSWORD}
./kcadm.sh create users -r springboot-microservice-realm -s username=user -s firstName=Name -s lastName=Surname -s email=name.surname@fake.com -s enabled=true
./kcadm.sh set-password -r springboot-microservice-realm --username user -p user