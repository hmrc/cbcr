package uk.gov.hmrc.cbcr.repositories


import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models.{CorrDocRefId, DocRefId, MessageRefId, UploadFileResponse}
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class MessageRefIdRepositorySpec extends UnitSpec with MockAuth with OneAppPerSuite {

  val config                  = app.injector.instanceOf[Configuration]
  implicit val ec             = app.injector.instanceOf[ExecutionContext]
  implicit val hc             = HeaderCarrier()
  val writeResult             = DefaultWriteResult(true,1,Seq.empty,None,None,None)
  val notFoundWriteResult     = DefaultWriteResult(true,0,Seq.empty,None,None,None)
  lazy val reactiveMongoApi   = app.injector.instanceOf[ReactiveMongoApi]
  val messageRefIdRepository      = new MessageRefIdRepository(reactiveMongoApi)
  //val fir = UploadFileResponse("id1", "fid1", "status",None)


  "Calls to Save  MessageRefId" should {
  "should successfully save that MessageRefId" in {

  val result: Future[WriteResult] = messageRefIdRepository.save(MessageRefId("mRId"))
  await(result.map(r => r.ok)) shouldBe true

}
}

  "Calls to check exist a MessageRefId" should {
  "should available  that MessageRefId" in {

  val result: Future[Boolean] = messageRefIdRepository.exists("mRId")
  await(result) shouldBe true

}
}
  "Calls to check exist a MessageRefId" should {
    "should not available that MessageRefId" in {

      val result: Future[Boolean] = messageRefIdRepository.exists("mRId1")
      await(result) shouldBe false

    }
  }

  "Calls to delete a MessageRefId which is not exist" should {
  "should delete that MessageRefId" in {

  val result: Future[WriteResult] = messageRefIdRepository.delete(MessageRefId("mRId"))
  await(result.map(r=> r.ok)) shouldBe  true

}
}



}
