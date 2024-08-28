#include "SoftwareVersion.hh"

namespace BackupRestoreAgent
{

SoftwareVersion::SoftwareVersion(const std::string& productName,
    const std::string& productNumber,
    const std::string& revision,
    const std::string& productionDate,
    const std::string& description,
    const std::string& type)
    : productName(productName)
    , productNumber(productNumber)
    , revision(revision)
    , productionDate(productionDate)
    , description(description)
    , type(type)
{
}

SoftwareVersion::SoftwareVersion(SoftwareVersionInfo versionInfo)
    : productName(versionInfo.productname())
    , productNumber(versionInfo.productnumber())
    , revision(versionInfo.revision())
    , productionDate(versionInfo.productiondate())
    , description(versionInfo.description())
    , type(versionInfo.type())
{
}

std::string SoftwareVersion::getProductName()
{
    return productName;
}

std::string SoftwareVersion::getProductNumber()
{
    return productNumber;
}

std::string SoftwareVersion::getRevision()
{
    return revision;
}

std::string SoftwareVersion::getProductionDate()
{
    return productionDate;
}

std::string SoftwareVersion::getDescription()
{
    return description;
}

std::string SoftwareVersion::getType()
{
    return type;
}

SoftwareVersion::operator SoftwareVersionInfo() const
{
    SoftwareVersionInfo softwareVersionInfo;
    softwareVersionInfo.set_productname(productName);
    softwareVersionInfo.set_productnumber(productNumber);
    softwareVersionInfo.set_revision(revision);
    softwareVersionInfo.set_productiondate(productionDate);
    softwareVersionInfo.set_description(description);
    softwareVersionInfo.set_type(type);
    return softwareVersionInfo;
}

bool SoftwareVersion::operator==(const SoftwareVersion& other)
{
    return this->productName == other.productName && this->productNumber == other.productNumber && this->revision == other.revision && this->productionDate == other.productionDate && this->description == other.description && this->type == other.type;
}

bool SoftwareVersion::operator!=(const SoftwareVersion& other)
{
    return !this->operator==(other);
}
}
