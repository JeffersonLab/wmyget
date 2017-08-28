# myquery
MYA Query Web Service

Uses [jmyapi](https://github.com/JeffersonLab/jmyapi) to provide a web service for fetching data from the JLab Archiver 'MYA'.

## API    

### Multiple Event Query (Interval)   
Query for all events on the timeline between the begin (inclusive) and end (exclusive) dates.

_**Path:** myquery/interval_   

**Request URL Parameters**     

| Name  | Description                                                  | Value Format                                        | Required | Default                                    |   
|-------|--------------------------------------------------------------|-----------------------------------------------------|----------|--------------------------------------------|   
| c     | EPICS Channel name                                           | String                                              | YES      |                                            |  
| b     | Inclusive begin date with optional time                      | String in ISO 8601 format (YYYY-MM-DD[Thh:mm:[ss]]) | YES      |                                            |  
| e     | Exclusive end date with optional time                        | String in ISO 8601 format (YYYY-MM-DD[Thh:mm:[ss]]) | YES      |                                            |
| l     | Limit by binning / sampling                                  | Boolean, true if parameter exists                   | NO       | No sampling is done                        | 
| m     | MYA deployment                                               | String                                              | NO       | 'ops'                                      |   
| M     | MYA master host override                                     | String                                              | NO       | Standard master used                       |   
| d     | Expression to filter events                                  | String                                              | NO       | No filter applied                          |   
| f     | Fractional seconds time digits                               | Integer (0-6)                                       | NO       | 0 (ISO 8601 only)                          |    
| v     | Fractional floating value digits                             | Integer (0-9)                                       | NO       | 6 (floats only)                            |   
| p     | Include prior point (guarantee at least one point in result) | Boolean, true if parameter exists                   | NO       | Prior point isn't included                 |   
| s     | Enumerations as strings                                      | Boolean, true if parameter exists                   | NO       | Enumerations presented as ordinal number   |   
| u     | Timestamps as milliseconds from UNIX Epoch                   | Boolean, true if parameter exists                   | NO       | Timestamps are returned in ISO 8601 format |   

**Response JSON Format**    
*On Success (HTTP 200 Response Code):*   
````json
{   
    "datatype":"<EPICS datatype>",     
    "datasize":"<data vector size; 1 for scalar>",    
    "datahost":"<MYA hostname of data home>",      
    "sampled":"<true if sampled, false otherwise>", 
    "count":"<original count of events; only present if sampled = true>",
    "data":[   
        {   
            "d":"<DATE-TIME>",   
            "v":"<VALUE>"   
        },   
        ...   
    ]    
}     
````

*On Error (HTTP 400 Repsonse Code):*    
````json
{   
    "error":"<error reason>"   
}      
````

### Single Event Query (Point)
Query for a single event on the timeline closest to the specified point.  The direction to search from the point is determined by the 'w' parameter.

_**Path:** myquery/point_    

**Request Parameters**     

| Name  | Description                                                  | Value Format                                        | Required | Default                                    |   
|-------|--------------------------------------------------------------|-----------------------------------------------------|----------|--------------------------------------------|   
| c     | EPICS Channel name                                           | String                                              | YES      |                                            |
| t     | Time of interest date with optional time                     | String in ISO 8601 format (YYYY-MM-DD[Thh:mm:[ss]]) | YES      |                                            |
| m     | MYA deployment                                               | String                                              | NO       | 'ops'                                      |   
| M     | MYA master host override                                     | String                                              | NO       | Standard master used                       |
| d     | Expression to filter events                                  | String                                              | NO       | No filter applied                          |   
| f     | Fractional seconds time digits                               | Integer (0-6)                                       | NO       | 0 (ISO 8601 only)                          |
| v     | Fractional floating value digits                             | Integer (0-9)                                       | NO       | 6 (floats only)                            |  
| w     | Get closest event greater than time of interest              | Boolean, true if parameter exists                   | NO       | Get closest event less than time of interest   |
| x     | Closest event is exclusive of time of interest               | Boolean, true if parameter exists                   | NO       | Closest event is inclusive of time of interest |
| s     | Enumerations as strings                                      | Boolean, true if parameter exists                   | NO       | Enumerations presented as ordinal number   |
| u     | Timestamps as milliseconds from UNIX Epoch                   | Boolean, true if parameter exists                   | NO       | Timestamps are returned in ISO 8601 format | 

**Response JSON Format**   
*On Success (HTTP 200 Response Code):*   
````json
{   
    "datatype":"<EPICS datatype>",     
    "datasize":"<data vector size; 1 for scalar>",    
    "datahost":"<MYA hostname of data home>",  
    "data":{   
        "d":"<DATE-TIME>",   
        "v":"<VALUE>"    
        }   
}    
````

*On Error (HTTP 400 Repsonse Code):*    
````json
{   
    "error":"<error reason>"   
}       
````

### JSONP
The API also supports JSONP responses.  Simply provide an additional parameter:

````
jsonp=<function name>
````
and the jsonp function name returned will be _&lt;function name&gt;_.
