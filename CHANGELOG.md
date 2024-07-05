# Changelog

## [2.5.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/compare/v2.4.0...v2.5.0) (2024-07-05)


### Features

* app icon added ([#11](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/issues/11)) ([1c3064a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/1c3064a863b03fe2bcac4d2e0954861e128745b8))


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.0.2 ([2feb16b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/2feb16bdbc65ae3a6fd6d551167d549bfb0ef2fe))
* Fixed null columnsMapping and field not existing in fieldMetadat… ([#7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/issues/7)) ([28ef99a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/28ef99aebeaadd38c4a0a220b2758d77d2f5eb8e)), closes [#6](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/issues/6)

## 2.4.0 (2024-07-03)


### Miscellaneous Chores

* migration from BitBucket ([#3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/issues/3)) ([ad17388](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/ad17388a144aac0ebe1524f62835808ec82309a0))

## Changelog before migration to conventional commits

| Version | Changes                                                                                                                                                                      |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| v2.3.1  | Fix getting custom fields from global configuration                                                                                                                          |
| v2.3.0  | Skip unused columns during excel file parsing<br/> Excel formulas support<br/> Proper formula errors handling                                                                |
| v2.2.1  | Swagger UI has ability to upload files for import operation                                                                                                                  |
| v2.2.0  | Do not return default settings if requested name does not exists                                                                                                             |
| v2.1.4  | Counting unchanged work items (tested types/fields: String, Boolean, Date, Integer, Float, Text, Rich Text, Enum, title, description. other fields are untested)             |
| v2.1.3  | Fixed LinkColumn field overwrite: Work-Item field value is only not overwritten if the field is id                                                                           |
| v2.1.2  | Error and success messages after the import don't vanish anymore<br/> Work-Item value of LinkColumn field is not overwritten anymore (faulty implementation!)                |
| v2.1.1  | Multi-value enumeration & date fields fix. 'Mappings' page: 'Default' button removed                                                                                         |
| v2.1.0  | Multi-value enumeration fields: comma-separated values support<br/> New mapping settings feature: 'Overwrite with empty values'                                              |
| v2.0.0  | Named mappings has been introduced<br/> Breaking change modifying the way settings stored<br/> New data types support added<br/> Import is now available for non-admin users |
| v1.2.0  | Apache POI v5.2.4 is used as external OSGi bundle                                                                                                                            |
| v1.1.1  | Enumeration support extended: standard fields are supported now                                                                                                              |
| v1.1.0  | Enumeration support added                                                                                                                                                    |
| v1.0.0  | Initial version of Excel Importer                                                                                                                                            |
