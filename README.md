# cbcr


[![Build Status](https://travis-ci.org/hmrc/cbcr.svg)](https://travis-ci.org/hmrc/cbcr) [ ![Download](https://api.bintray.com/packages/hmrc/releases/cbcr/images/download.svg) ](https://bintray.com/hmrc/releases/cbcr/_latestVersion)

The CBCR Service responsible for interacting with the Country-By-Country-Reporting Domain and interfacing with relevant HODS.

All endpoints are auth protected and for private use within the CBCR Domain.

## API
| URI                              | Http Method |Description                                               |Json          |
|:---------------------------------|:------------|:---------------------------------------------------------|:-------------|
|/file-upload-response             |POST         |Create a new FileUpload Response                          |[UploadFileResponse](#user-content-fileuploadresponse)|
|/file-upload-response/:envelopeId |GET          |Returns an existing FileUpload response by its EnvelopeId |[UploadFileResponse](#user-content-fileuploadresponse)|
|/subscription-data                |POST         |Create a nw SubscriptionData Entity                       |[SubscriptionDetails](#user-content-subscriptiondetails)|
|/subscription-data/:cbcid         |PUT          |Update an existing SubscriptionData Entiy by CBCid        |[SubscriptionDetails](#user-content-subscriptiondetails)|
|/subscription-data/cbc-id/:cbcid  |GET          |Returns an existing SubscriptionData by CBCid             |[SubscriptionDetails](#user-content-subscriptiondetails)|
|/subscription-data/utr/:utr       |GET          |Returns an existing SubscriptionData by Utr               |[SubscriptionDetails](#user-content-subscriptiondetails)|
|/subscription-data/:cbcid         |DELETE       |Delete an existing SubscriptionData by CBCid              |              |
|/business-partner-record/:utr     |GET          |Returns an existing BusinessPartnerRecord by Utr          |[BusinessPartnerRecord](#user-content-businesspartnerrecord)|
|/subscription                     |POST         |Create a new Subscription                                 |[SubscriptionDetails](#user-content-subscriptiondetails)|
|/subscription/:safeId             |GET          |Return an existing Subscription                           |[SubscriptionDetails](#user-content-subscriptiondetails)|
|/subscription/:safeId             |PUT          |Update an existing Subscritpion                           |[SubscriptionDetails](#user-content-subscriptiondetails)|
|/message-ref-id/:id               |PUT          |Update an existing MessageRefId Entity                    |[MessageRefId](#user-content-messagerefid)|
|/message-ref-id/:id               |GET          |Return an existing MessageRefId Entity                    |[MessageRefId](#user-content-messagerefid)|
|/doc-ref-id/:id                   |POST         |Create a DocRefId Entity                                  |[DocRefID](#user-content-docrefid)|
|/doc-ref-id/:id                   |GET          |Return an existing DocRefId Entity                        |[DocRefID](#user-content-docrefid)|
|/doc-ref-id/:id                   |PUT          |Update an existing DocRefId Entity                        |[DocRefID](#user-content-docrefid)|
|/corr-doc-ref-id/:cid/:id         |PUT          |Update an existing CorrDocRefId Entity                    ||
|/reporting-entity/:id             |GET          |Return an existing ReportingEntity                        |[ReportingEntityData](#user-content-reportingentitydata)|
|/reporting-entity                 |POST         |Create a ReportingEntity                                  |[ReportingEntityData](#user-content-reportingentitydata)|
|/reporting-entity                 |PUT          |Update an existing ReportingEntity                        |[ReportingEntityData](#user-content-reportingentitydata)|
|/email                            |POST         |Create a new CBCR email                                   ||                                     


## Json

### FileUploadResponse
```json
{
  "envelopeId": "df09bb9b-b0b2-4a8a-9576-8ecd2fedc602",
  "fileId": "f858bb69-6e01-44a2-aeb2-51c660f2d4c0",
  "status": "AVAILABLE",
  "reason": "foo"
}
```
### SubscriptionDetails

```json
{
  "businessPartnerRecord": {
    "safeId": "X1234567788",
    "organisation": {
      "organisationName": "Foo Ltd"
    },
    "address": {
      "addressLine1": "1 The Street",
      "countryCode": "GB"
    }
  },
  "subscriberContact": {
    "firstName": "Joesph",
    "lastName": "Blogger",
    "phoneNumber": "123456",
    "email": "joesph.blogger@gmail.com"
  },
  "cbcId": "XTCBC0100000001",
  "utr": "7000000002"
}
```

### Business Partner Record
```json
{
  "safeId": "X1234567788",
  "organisation": {
    "organisationName": "Foo Ltd"
  },
  "address": {
    "addressLine1": "1 The Street",
    "countryCode": "GB"
  }
}
```

### MessageRefId

```json
{
  "messageRefId": "GB2016RGXBCBC0100100000CBC40120170311T090000X"
}
```

### DocRefId
```json
{
 "id": "GB2016RGXBCBC0100100000CBC40120170311T090000X_5000000020OECD1ENT"
}
```

### ReportingEntityData
```json
{
  "cbcReportsDRI": [
    "GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP"
  ],
  "additionalInfoDRI": "GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP",
  "reportingEntityDRI": "GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP",
  "tin": "90000000001",
  "ultimateParentEntity": "Foo Corp",
  "reportingRole": "CBC701"
}
```

## Running

```sbtshell
sbt 'run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes'
```

=======


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

