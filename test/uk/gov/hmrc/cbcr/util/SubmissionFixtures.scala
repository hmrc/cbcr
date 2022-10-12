/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.cbcr.util

object SubmissionFixtures {

  val minimalPassing =
    <submission>
     <fileName>my-file.xml</fileName>
     <file>
       <ns1:CBC_OECD version="2.0" xmlns:ns1="urn:oecd:ties:cbc:v2">
         <ns1:MessageSpec>
           <ns1:SendingEntityIN>XLCBC0100000056</ns1:SendingEntityIN>
           <ns1:TransmittingCountry>US</ns1:TransmittingCountry>
           <ns1:ReceivingCountry>GB</ns1:ReceivingCountry>
           <ns1:MessageType>CBC</ns1:MessageType>
           <ns1:Language>EN</ns1:Language>
           <ns1:Warning>String_Warning</ns1:Warning>
           <ns1:Contact>Contact ABCCorp</ns1:Contact>
           <ns1:MessageRefId>GB2016RGXLCBC0100000056CBC40120170311T090000X</ns1:MessageRefId>
           <ns1:MessageTypeIndic>CBC402</ns1:MessageTypeIndic>
           <ns1:ReportingPeriod>2016-03-31</ns1:ReportingPeriod>
           <ns1:Timestamp>2016-11-01T15:00:00</ns1:Timestamp>
         </ns1:MessageSpec>
         <ns1:CbcBody>
           <ns1:ReportingEntity>
             <ns1:Entity>
               <ns1:ResCountryCode>GB</ns1:ResCountryCode>
               <ns1:TIN issuedBy="GB">7000000002</ns1:TIN>
               <ns1:IN INType="2001" issuedBy="GB">TEST</ns1:IN>
               <ns1:Name>ABCCorp</ns1:Name>
               <ns1:Address legalAddressType="OECD303">
                 <ns1:CountryCode>US</ns1:CountryCode>
                 <ns1:AddressFree>Free Text</ns1:AddressFree>
               </ns1:Address>
             </ns1:Entity>
             <ns1:ReportingRole>CBC703</ns1:ReportingRole>
             <ns1:ReportingPeriod>
               <ns1:StartDate>2015-04-01</ns1:StartDate>
               <ns1:EndDate>2016-03-31</ns1:EndDate>
             </ns1:ReportingPeriod>
             <ns1:DocSpec>
               <ns2:DocTypeIndic xmlns:ns2="urn:oecd:ties:cbcstf:v5">OECD2</ns2:DocTypeIndic>
               <ns2:DocRefId xmlns:ns2="urn:oecd:ties:cbcstf:v5">GB2016RGXLCBC0100000056CBC40120170311T090000X_7000000002OECD1ENT</ns2:DocRefId>
               <ns2:CorrDocRefId xmlns:ns2="urn:oecd:ties:cbcstf:v5">GB2016RGXMCBC0100000057CBC40120170311T090000X_7000000002OECD1ENT</ns2:CorrDocRefId>
             </ns1:DocSpec>
           </ns1:ReportingEntity>
           <ns1:CbcReports>
             <ns1:DocSpec>
               <ns2:DocTypeIndic xmlns:ns2="urn:oecd:ties:cbcstf:v5">OECD1</ns2:DocTypeIndic>
               <ns2:DocRefId xmlns:ns2="urn:oecd:ties:cbcstf:v5">GB2016RGXLCBC0100000056CBC40120170311T090000X_7000000002OECD1REP</ns2:DocRefId>
             </ns1:DocSpec>
             <ns1:ResCountryCode>US</ns1:ResCountryCode>
             <ns1:Summary>
               <ns1:Revenues>
                 <ns1:Unrelated currCode="USD">601</ns1:Unrelated>
                 <ns1:Related currCode="USD">602</ns1:Related>
                 <ns1:Total currCode="USD">603</ns1:Total>
               </ns1:Revenues>
               <ns1:ProfitOrLoss currCode="USD">604</ns1:ProfitOrLoss>
               <ns1:TaxPaid currCode="USD">605</ns1:TaxPaid>
               <ns1:TaxAccrued currCode="USD">606</ns1:TaxAccrued>
               <ns1:Capital currCode="USD">607</ns1:Capital>
               <ns1:Earnings currCode="USD">608</ns1:Earnings>
               <ns1:NbEmployees>609</ns1:NbEmployees>
               <ns1:Assets currCode="USD">610</ns1:Assets>
             </ns1:Summary>
             <ns1:ConstEntities>
               <ns1:ConstEntity>
                 <ns1:ResCountryCode>US</ns1:ResCountryCode>
                 <ns1:TIN issuedBy="US">3001</ns1:TIN>
                 <ns1:IN INType="4001" issuedBy="US">TEST</ns1:IN>
                 <ns1:Name/>
                 <ns1:Address legalAddressType="OECD303">
                   <ns1:CountryCode>US</ns1:CountryCode>
                   <ns1:AddressFree>Free Text</ns1:AddressFree>
                 </ns1:Address>
               </ns1:ConstEntity>
               <ns1:IncorpCountryCode>US</ns1:IncorpCountryCode>
               <ns1:BizActivities>CBC510</ns1:BizActivities>
               <ns1:OtherEntityInfo>String_OtherEntityInfo</ns1:OtherEntityInfo>
             </ns1:ConstEntities>
           </ns1:CbcReports>
           <ns1:AdditionalInfo>
             <ns1:DocSpec>
               <ns2:DocTypeIndic xmlns:ns2="urn:oecd:ties:cbcstf:v5">OECD2</ns2:DocTypeIndic>
               <ns2:DocRefId xmlns:ns2="urn:oecd:ties:cbcstf:v5">GB2016RGXLCBC0100000056CBC40120170311T090000X_7000000002OECD1ADD</ns2:DocRefId>
               <ns2:CorrDocRefId xmlns:ns2="urn:oecd:ties:cbcstf:v5">GB2016RGXMCBC0100000057CBC40120170311T090000X_7000000002OECD1ADD</ns2:CorrDocRefId>
             </ns1:DocSpec>
             <ns1:OtherInfo>String_OtherInfo</ns1:OtherInfo>
             <ns1:ResCountryCode>GB</ns1:ResCountryCode>
             <ns1:SummaryRef>CBC610</ns1:SummaryRef>
           </ns1:AdditionalInfo>
         </ns1:CbcBody>
       </ns1:CBC_OECD>
     </file>
   </submission>

}
