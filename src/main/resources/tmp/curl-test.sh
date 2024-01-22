#!/bin/bash -x

#clientId="7ba598c2-c4c3-45bf-8c82-d6324c842c18"
#client_secret="AK2VteBDq0rcDFycJjqz0qUMaCTHQ7phd0mFQum1vemayDczreA38d344p9L5lDMX6oImNzl3_4MdjrZ0uk-EDY"

#echo $1

#browser: https://oidc.sandbox.bankid.cz/auth?client_id=7ba598c2-c4c3-45bf-8c82-d6324c842c18&redirect_uri=http%3A%2F%2F127.0.0.1:8080%2Fcallback&scope=profile.birthnumber%20profile.phonenumber%20profile.zoneinfo%20profile.gender%20openid%20profile.titles%20profile.name%20profile.birthplaceNationality%20profile.locale%20profile.idcards%20profile.maritalstatus%20profile.legalstatus%20profile.email%20profile.paymentAccounts%20profile.addresses%20profile.birthdate%20profile.updatedat&response_type=code&state=BankID%20works%21&nonce=fd68ae30-15d5-4937-be1f-f0a5ac071422&prompt=login&display=page&acr_values=loa2

#curl -v --location --request POST 'https://oidc.sandbox.bankid.cz/token' \
#    --header 'Content-Type: application/x-www-form-urlencoded' \
#    --data-urlencode 'grant_type=authorization_code' \
#    --data-urlencode 'client_id=7ba598c2-c4c3-45bf-8c82-d6324c842c18' \
#    --data-urlencode 'client_secret=AK2VteBDq0rcDFycJjqz0qUMaCTHQ7phd0mFQum1vemayDczreA38d344p9L5lDMX6oImNzl3_4MdjrZ0uk-EDY' \
#    --data-urlencode 'redirect_uri=http://127.0.0.1:8080/callback' \
#    --data-urlencode 'code=ZH58AB--2vfjqRd3Y_tV7O'|jq

curl --location --request POST 'http://127.0.0.1:8080/c2id/token' \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode 'grant_type=authorization_code' \
    --data-urlencode 'client_id=000123' \
    --data-urlencode 'client_secret=7wKJNYFaKKg4FxUdi8_R75GGYsiWezvAbcdN1uSumE4' \
    --data-urlencode 'redirect_uri=http://127.0.0.1:8080/oidc-client/cb' \
    --data-urlencode 'code=$1'

echo
