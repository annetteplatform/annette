package biz.lobachev.annette.microservice_core.test.indexing

import biz.lobachev.annette.core.exception.AnnetteTransportException
import biz.lobachev.annette.microservice_core.indexing._
import biz.lobachev.annette.microservice_core.indexing.config._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IndexConfigSpec extends AnyWordSpec with Matchers {

  "ElasticConnectionConfig" should {
    "load config" in {
      val configPath = ConnectionConfigPath("indexing.connection")
      val config     = IndexingProvider.loadConnectionConfig(configPath)
      println(config)
      config shouldBe ConnectionConfig(
        "https://localhost:9200",
        Some("admin"),
        Some("admin"),
        false
      )
    }

    "load config with error" in {
      val configPath = ConnectionConfigPath("failConnection")
      the[AnnetteTransportException] thrownBy {
        IndexingProvider.loadConnectionConfig(configPath)
      } should have message s"500 (Unexpected Condition/Internal Server Error) - ${ConnectionConfigError.MessageCode}[path: failConnection, description: Key not found: 'url'.]"
    }

    "load nonexisting config" in {
      val configPath = ConnectionConfigPath("indexing.failConnection")
      the[AnnetteTransportException] thrownBy {
        IndexingProvider.loadConnectionConfig(configPath)
      } should have message s"500 (Unexpected Condition/Internal Server Error) - ${ConnectionConfigError.MessageCode}[path: indexing.failConnection, description: Key not found: 'failConnection'.]"
    }

  }
  "IndexConfig" should {
    "load config" in {
      val config = IndexingProvider.loadIndexConfig("indexing.person-category-index")

      config shouldBe IndexConfig(
        index = "person-category",
        mappings = Map(
          "id"        -> KeywordField(
            field = Some("id")
          ),
          "updatedAt" -> DateField(
            field = Some("updatedAt")
          ),
          "name"      -> TextField(
            field = Some("name"),
            fielddata = true,
            analyzer = Some("name_analyzer"),
            searchAnalyzer = Some("standard"),
            fields = Map(
              "keyword" -> KeywordField(None),
              "english" -> TextField(
                field = Some("english"),
                fielddata = false,
                analyzer = Some("english"),
                searchAnalyzer = None,
                fields = Map()
              )
            )
          )
        ),
        analyzers = Map(
          "name_analyzer" -> CustomAnalyzerConf(
            tokenizer = "name_tokenizer",
            charFilters = List("filter"),
            tokenFilters = List("lowercase"),
            positionIncrementGap = 100
          ),
          "stop"          -> StopAnalyzerConf(
            stopwords = List("and", "or", "not")
          ),
          "fingerprint"   -> FingerprintAnalyzerConf(
            separator = Some(","),
            stopwords = List("and", "or", "not"),
            maxOutputSize = 100
          ),
          "pattern"       -> PatternAnalyzerConf(
            regex = "regex",
            lowercase = false
          )
        ),
        tokenizers = Map(
          "Pattern"       -> PatternTokenizerConf(
            pattern = "pattern",
            flags = "f",
            group = 10
          ),
          "Thai"          -> ThaiTokenizerConf(),
          "Standard"      -> StandardTokenizerConf(
            maxTokenLength = 100
          ),
          "ICU"           -> ICUTokenizerConf(
            ruleFiles = "asdf"
          ),
          "EdgeNGram"     -> EdgeNGramTokenizerConf(
            minGram = 2,
            maxGram = 50,
            tokenChars = List("a", "b", "C")
          ),
          "Whitespace"    -> WhitespaceTokenizerConf(
            maxTokenLength = 50
          ),
          "PathHierarchy" -> PathHierarchyTokenizerConf(
            delimiter = '-',
            replacement = '+',
            bufferSize = 100,
            reverse = false,
            skip = 0
          ),
          "NGram"         -> NGramTokenizerConf(
            minGram = 3,
            maxGram = 30,
            tokenChars = List("a", "b", "d")
          ),
          "Keyword"       -> KeywordTokenizerConf(
            bufferSize = 256
          ),
          "UaxUrlEmail"   -> UaxUrlEmailTokenizerConf(
            maxTokenLength = 100
          ),
          "CharGroup"     -> CharGroupTokenizerConf(
            tokenizeOnChars = List("q", "w", "e")
          ),
          "Classic"       -> ClassicTokenizerConf(
            maxTokenLength = 30
          )
        )
      )
    }

    "duplicated fields" in {
      the[AnnetteTransportException] thrownBy {
        IndexingProvider.loadIndexConfig("indexing.duplicated-fields")
      } should have message s"500 (Unexpected Condition/Internal Server Error) - ${DuplicateIndexFields.MessageCode}[fields: name, duplicated1]"

    }

  }

}
