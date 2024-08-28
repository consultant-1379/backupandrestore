#pragma once

#include <string>

#include "SoftwareVersionInfo.pb.h"

namespace BackupRestoreAgent
{

/**
 * Holds agent's software version.
 */
class SoftwareVersion
{
public:
    /**
     * Provides a constructor for the software version. This is sent to the orchestrator during
     * registration and is stored alongside any backups created.
     *
     * @param productName The name of the product
     * @param productNumber The product number for example "APR20140/1"
     * @param revision The revision of the product or example "R1A"
     * @param productionDate The production date of the software
     * @param description The software description
     * @param type The type of software
     */
    explicit SoftwareVersion(const std::string& productName,
        const std::string& productNumber,
        const std::string& revision,
        const std::string& productionDate,
        const std::string& description,
        const std::string& type);
    SoftwareVersion(SoftwareVersionInfo versionInfo);

    std::string getProductName();
    std::string getProductNumber();
    std::string getRevision();
    std::string getProductionDate();
    std::string getDescription();
    std::string getType();

    operator SoftwareVersionInfo() const;
    bool operator==(const SoftwareVersion& other);
    bool operator!=(const SoftwareVersion& other);

private:
    std::string productName;
    std::string productNumber;
    std::string revision;
    std::string productionDate;
    std::string description;
    std::string type;

};

}
