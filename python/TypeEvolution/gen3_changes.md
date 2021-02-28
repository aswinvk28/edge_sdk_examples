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
        
## File: TemperatureSensor.py

MOD2. In TemperatureSensor.py, modify writeSample() to create and write humidity as well as temperature Temperature object:

	    def write_sample(self, temperature, humidity):
	        sensor_data = Temperature()
	        sensor_data.temperature = temperature
	        sensor_data.humidity = humidity
	        self._thing.write('temperature', sensor_data)
        

MOD3. In TemperatureSensor.py, re-write run() to configure a random number generator
for humidity values, so that the fall uniformly between 20 and 100%:

	    def run(self, running_time):
	        random.seed()
	        sample_count = (float(running_time) * 1000.0) / SAMPLE_DELAY_MS
	        actual_temperature = 21.5
	 
	        while sample_count > 0:
	            # Simulate temperature change
	            actual_temperature += float(random.randrange(10) - 5) / 5.0
	            actual_humidity = random.uniform(20.0, 100.0)
	 
	            self.write_sample(actual_temperature, actual_humidity)
	            
	            time.sleep(SAMPLE_DELAY_MS/1000.0)
	            sample_count = sample_count - 1.0


## File:: TemperatureDashboard.py

MOD4. In TemperatureDashboard.cpp, method run(), modify the calculation of Gen2 sensors to identify Gen3 sensors, too. 
A Gen3 sensor will have a non-zero humidity value:

		if msg.is_compatible(Temperature):
			sensor_data = msg.get(Temperature)
			if(sensor_data.humidity == 0.0):
				sensor_generation = "Gen2"
			else:
				sensor_generation = "Gen3"
		else: 
			sensor_generation = "Gen1"
			data_sample = msg.get(IotNvpSeq)
			try:
				# Get temperature value from sample
				for nvp in data_sample:
					if nvp.name == 'temperature':
						temperature = nvp.value.float32
			except Exception as e:
				print('An unexpected error occured while processing data-sample: ' + str(e))
				continue

MOD5. In TemperatureDashboard.py, method run(), enhance the printing of sensor data to include non-zero humidity values:

		# Show output
		if(sensor_generation == "Gen1"):
			print('Temperature data received for flow {} ({}): {:5.2f}'.format(str(msg.flow_id), sensor_generation, temperature))
		elif(sensor_generation == "Gen2"):
			print('Temperature data received for flow {} ({}): {:5.2f}'.format(str(msg.flow_id), sensor_generation, sensor_data.temperature))
		else:
			print('Temperature data received for flow {} ({}): {:5.2f}, {:5.2f}'.format(str(msg.flow_id), sensor_generation, sensor_data.temperature, sensor_data.humidity))
  

