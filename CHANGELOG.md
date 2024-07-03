# Changelog before migration to conventional commits

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
