# LaConvDt

Utility to convert between various date types.

Functions general format `LaConvDt.convTo<DateType>(<FromDateType> date)`:
- LaConvDt.convToLocalDate(...);
- LaConvDt.convToLocalDateTime(...);
- LaConvDt.convToOffsetDateTime(...);
- LaConvDt.convToZonedDateTime(...);
- LaConvDt.convToInstant(...);
- LaConvDt.convToDate(...);

#### Local time vs. offset time 

Dates are from system zone point of view, i.e. the systemZone is used when converting LocalDateTime/LocalDate to OffsetDateTime/ZonedDateTime.

An example:

Assuming system zone offset is +02:00, then
date _OffsetDateTime_ `2018-11-08T00:00:00+10:00` 
will become _LocalDateTime_ `2018-11-07T16:00:00`,
or _LocalDate_ `2018-11-07` (i.e., the day is different)
    
