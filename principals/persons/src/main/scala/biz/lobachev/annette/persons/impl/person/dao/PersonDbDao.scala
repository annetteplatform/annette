package biz.lobachev.annette.persons.impl.person.dao

import biz.lobachev.annette.core.attribute.AttributeValues
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.persons.api.person.Person

import scala.concurrent.Future

trait PersonDbDao {

  def getPersonById(id: PersonId, attributes: Seq[String]): Future[Option[Person]]
  def getPersonsById(ids: Set[PersonId], attributes: Seq[String]): Future[Seq[Person]]
  def getPersonAttributes(id: PersonId, attributes: Seq[String]): Future[Option[Map[String, String]]]
  def getPersonsAttributes(ids: Set[PersonId], attributes: Seq[PersonId]): Future[Map[String, AttributeValues]]

  protected def getAttributesById(id: PersonId, attributes: Seq[String]): Future[AttributeValues]

  protected def getAttributesById(ids: Set[PersonId], attributes: Seq[String]): Future[Map[String, AttributeValues]]

}
