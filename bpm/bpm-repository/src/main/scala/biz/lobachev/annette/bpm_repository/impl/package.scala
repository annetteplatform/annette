package biz.lobachev.annette.bpm_repository

import slick.jdbc.PostgresProfile

package object impl {

  type PostgresDatabase = PostgresProfile.backend.Database

}
