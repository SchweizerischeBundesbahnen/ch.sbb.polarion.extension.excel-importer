# Changelog

## [2.6.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/compare/v2.6.0...v2.6.1) (2024-08-01)


### Bug Fixes

* migration to generic v6.5.2 ([#23](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/issues/23)) ([12fd785](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/12fd785c7132384750fb79cef9f13f75bd01ce0e))

## [2.6.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/compare/v2.5.0...v2.6.0) (2024-07-31)


### Features

* migration to generic v6.2.0 ([#15](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/issues/15)) ([487d902](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/487d902261d9e7455f4fce4e2eeca70275cf5205))
* migration to generic v6.5.1 ([#21](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/issues/21)) ([cda4b45](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/cda4b45faf55f4fabdf38bc04abce182ca88435d))


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.2.0 ([8aae965](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/8aae9657ee9fc649203744ef19dff8bdc9da36fd))
* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.3.0 ([6e9aa57](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/6e9aa572b6512cea7fe58ee4283a4295dbdd94a1))
* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.4.0 ([ae75e45](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/ae75e45629f88ec152bf1a0188396c23edd604ef))
* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.5.0 ([fe3a1a3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/fe3a1a3a4e9bc9d027bf3f3f10fba2f63ff87c1c))
* UTF-8 for about.jsp ([#18](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/issues/18)) ([d809d1b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/d809d1bdd825b86a34d47b1e5a62d71f2053bc05))


### Documentation

* README.md updated in section about ch.sbb.polarion.thirdparty.b… ([#13](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/issues/13)) ([ca29723](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.excel-importer/commit/ca29723fb7c90d65c70e837cb3384e2125d2f8b5))

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
