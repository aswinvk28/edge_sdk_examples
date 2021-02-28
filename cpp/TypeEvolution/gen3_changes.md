# About the Gen3 Application

The Gen2 application is a evolution of the Gen 2 application, making use of Google Protobuf type evolution. THe motivation of changes were as follows:

* As with Gen2, it is assumed that Gen3 must interoperate with previous generations.
* The Gen3 temperature sensor produces two values: temperature and humidity. The TemperatureTagGroup.proto file was modified accordingly.
* The tag group identifier for the protobuf Temperature tag group _has not changed_. Google Protobuf type evolution allows applications
that use different evolutions of the same type to inter-operate. Thus, from the DataRiver perspective, Gen2 and Gen3 sensors use the same
tag group definition.
* The dash board application distinguishes between Gen2 and Gen3 sensors by looking for a zero value in the humidity field. A non-zero value
indicates a Gen3 sensor.

Note that inter-operation with Google Protobuf type evolutions works both ways. You could run a Gen2 dash board with Gen3 sensors, and still receive
temperature data. Because the Gen2 dash board is unaware of the Gen3 humidity field, it would not handle this field.

# Changes made to Gen2_Sensor_Dashboard

## File: definitions/TemperatureTagGroup.proto


MOD1. Added a new field, humidity:

        syntax = "proto3";
        
        import "adlinktech/datariver/descriptor.proto";
        
        package com.adlinktech.example;
        
        
        message Temperature {
          option(.adlinktech.datariver.tag_group) = {
            qosProfile: "telemetry"
            description: "ADLINK Edge SDK Example Temperature TagGroup"
            version: "v2.0"
          };
        
          float temperature = 1 [(.adlinktech.datariver.field_options) = {
            description: "Temperature"
            unit: "°C"
          }];
          float humidity = 2 [(.adlinktech.datariver.field_options) = {
            description: "Relative Humidity"
            unit: "%"
          }];
        };

## File: src/TemperatureSensor.cpp

MOD2. In TemperatureSensor.cpp, modify writeSample() to create and write humidity as well as temperature Temperature object:

        void writeSample(const float temperature, const float humidity) {
            Temperature sensorData;
    
            sensorData.set_temperature(temperature);
            sensorData.set_humidity(humidity);
    
            m_thing.write("temperature", sensorData);
        }

MOD3. In TemperatureSensor.cpp, re-write run() to configure a C++ 11 `uniform_real_distribution` random number generator
for humidity values, so that the fall uniformly between 20 and 100%:

       int run(int runningTime) {
            srand((unsigned int)time(NULL));
            // init random generator for humidity
            std::random_device humidity_seed;  //Will be used to obtain a seed for the random number engine
            std::mt19937 gen(humidity_seed()); //Standard mersenne_twister_engine seeded with humidity_seed()
            // generate humidity values between 20 and 100
            std::uniform_real_distribution<> humidity_dist(20.0, 100.0);
    
            int sampleCount = (runningTime * 1000) / SAMPLE_DELAY_MS;
            float actualTemperature = 21.5f;
            float humidity;
    
            while (sampleCount-- > 0) {
                // Simulate temperature change
                actualTemperature += (float)(rand() % 10 - 5) / 5.0f;
                humidity = static_cast<float>(humidity_dist(gen));
    
                writeSample(actualTemperature, humidity);
    
                this_thread::sleep_for(chrono::milliseconds(SAMPLE_DELAY_MS));
            }
    
            return 0;
        }


## File:: src/TemperatureDashboard.cpp

MOD4. In TemperatureDashboard.cpp, method TemperatureDashboard::run(), modify the calculation of Gen2 sensors to identify Gen3 sensors, too. 
A Gen3 sensor will have a non-zero humidity value:

            msg.get(tempData);
            sensorGeneration = tempData.humidity() == 0 ? "Gen2" : "Gen3";

MOD5. In TemperatureDashboard.cpp, method TemperatureDashboard::run(), enhance the printing of sensor data to include non-zero humidity values:

            cout << "Temperature data received for flow "
                << msg.getFlowId() << "(" << sensorGeneration << "): "
                << fixed << setw(3) << setprecision(1)
                << tempData.temperature();
            if(tempData.humidity() > 0) {
                cout << ", " << fixed << setw(3) << setprecision(1)
                     << tempData.humidity() << "%";
            }
            cout << endl;

