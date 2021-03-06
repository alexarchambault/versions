package coursier.version

/**
 * Represents a reconciliation strategy given a dependency conflict.
 */
sealed abstract class VersionCompatibility {
  def isCompatible(constraint: String, version: String): Boolean
}

object VersionCompatibility {

  case object Default extends VersionCompatibility {
    def isCompatible(constraint: String, version: String): Boolean =
      PackVer.isCompatible(constraint, version)
  }

  case object Always extends VersionCompatibility {
    def isCompatible(constraint: String, version: String): Boolean =
      true
  }

  /**
    * Strict version reconciliation.
    *
    * This particular instance behaves the same as [[Default]] when used by
    * [[coursier.core.Resolution]]. Actual strict conflict manager is handled
    * by `coursier.params.rule.Strict`, which is set up by `coursier.Resolve`
    * when a strict reconciliation is added to it.
    */
  case object Strict extends VersionCompatibility {
    def isCompatible(constraint: String, version: String): Boolean =
      constraint == version || {
        val c = VersionParse.versionConstraint(constraint)
        val v = Version(version)
        if (c.interval == VersionInterval.zero)
          c.preferred.contains(v)
        else
          c.interval.contains(v)
      }
  }

  /**
    * Semantic versioning version reconciliation.
    *
    * This particular instance behaves the same as [[Default]] when used by
    * [[coursier.core.Resolution]]. Actual semantic versioning checks are handled
    * by `coursier.params.rule.Strict` with field `semVer = true`, which is set up
    * by `coursier.Resolve` when a SemVer reconciliation is added to it.
    */
  case object SemVer extends VersionCompatibility {
    def isCompatible(constraint: String, version: String): Boolean =
      constraint == version || {
        val c = VersionParse.versionConstraint(constraint)
        val v = Version(version)
        if (c.interval == VersionInterval.zero)
          // FIXME This is not actually sem ver
          c.preferred.exists(_.items.take(2) == v.items.take(2))
        else
          c.interval.contains(v)
      }
  }

  case object PackVer extends VersionCompatibility {
    def isCompatible(constraint: String, version: String): Boolean =
      constraint == version || {
        val c = VersionParse.versionConstraint(constraint)
        val v = Version(version)
        if (c.interval == VersionInterval.zero)
          c.preferred.exists(_.items.take(2) == v.items.take(2))
        else
          c.interval.contains(v)
      }
  }

  def apply(input: String): Option[VersionCompatibility] =
    input match {
      case "default" => Some(Default)
      case "always" => Some(Always)
      case "strict" => Some(Strict)
      case "semver" => Some(SemVer)
      case "pvp" => Some(PackVer)
      case _ => None
    }
}
