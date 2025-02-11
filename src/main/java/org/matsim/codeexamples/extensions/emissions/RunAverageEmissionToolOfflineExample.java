/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.codeexamples.extensions.emissions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.example.CreateEmissionConfig;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.MatsimVehicleWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 *
 * Use the config file as created by the
 * {@link CreateEmissionConfig CreateEmissionConfig} to calculate
 * emissions based on the link leave events of an events file. Resulting emission events are written into an event file.
 *
 * @author benjamin, julia
 */
public final class RunAverageEmissionToolOfflineExample{

	private static final String eventsFile =  "./scenarios/sampleScenario/5.events.xml.gz";

	private static final Logger log = LogManager.getLogger(RunAverageEmissionToolOfflineExample.class);

	/* package, for test */ static final String emissionEventOutputFileName = "_5.emission.events.offline.xml.gz";

	// =======================================================================================================

	public static void main (String[] args) throws IOException {
		// see testcase for an example
		Config config ;
		if ( args==null || args.length==0 || args[0]==null ) {
			config = ConfigUtils.loadConfig( "./scenarios/sampleScenario/testv2_Vehv2/config_average.xml" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		config.controler().setOutputDirectory( config.controler().getOutputDirectory() );
//		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );

		emissionsConfig.setAverageColdEmissionFactorsFile( "../sample_EFA_ColdStart_vehcat_2020_average_withHGVetc.csv" );
		emissionsConfig.setAverageWarmEmissionFactorsFile( "../sample_41_EFA_HOT_vehcat_2020average.csv" );

		emissionsConfig.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.directlyTryAverageTable );

		emissionsConfig.setNonScenarioVehicles( EmissionsConfigGroup.NonScenarioVehicles.abort );

		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// examples for how to set attributes to links and vehicles in order to make this work (already there for example scenario):

//		for( Link link : scenario.getNetwork().getLinks().values() ){
//			if ( true ) {
//				EmissionUtils.setHbefaRoadType( link, "URB/Local/50" );
//			}
//		}
//
//		for( VehicleType vehicleType : scenario.getVehicles().getVehicleTypes().values() ){
//			if ( true ){
//				VehicleUtils.setHbefaVehicleCategory( vehicleType.getEngineInformation(), HbefaVehicleCategory.PASSENGER_CAR.toString() );
//				VehicleUtils.setHbefaTechnology( vehicleType.getEngineInformation(), "average" );
//				VehicleUtils.setHbefaEmissionsConcept( vehicleType.getEngineInformation(), "average" );
//				VehicleUtils.setHbefaSizeClass( vehicleType.getEngineInformation(), "average");
//			}
//		}

		// ---

		// we do not want to run the full Controler.  In consequence, we plug together the infrastructure one needs in order to run the emissions contrib:

		EventsManager eventsManager = EventsUtils.createEventsManager();

		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};

		com.google.inject.Injector injector = Injector.createInjector( config, module );

		// the EmissionModule must be instantiated, otherwise it does not work:
		injector.getInstance(EmissionModule.class);

		// ---

		// add events writer into emissions event handler
		final EventWriterXML eventWriterXML = new EventWriterXML( config.controler().getOutputDirectory() + emissionEventOutputFileName );
		eventsManager.addHandler( eventWriterXML );

		// necessary for link emissions [g/m] output
		EmissionsOnLinkEventHandler emissionsOnLinkEventHandler = new EmissionsOnLinkEventHandler(10.);
		eventsManager.addHandler( emissionsOnLinkEventHandler );

		// read events file into the events reader. EmissionsModule, events writer and link emissions event handlers have been added, and will act accordingly.
		new MatsimEventsReader(eventsManager).readFile( eventsFile );

		// events writer needs to be explicitly closed, otherwise it does not work:
		eventWriterXML.closeFile();

		// also write vehicles and network as a service so we have all out files in one directory:
		new MatsimVehicleWriter( scenario.getVehicles() ).writeFile( config.controler().getOutputDirectory() + "_vehicles.xml.gz" );
		NetworkUtils.writeNetwork( scenario.getNetwork(), config.controler().getOutputDirectory() + "_network.xml.gz" );


		{ // writing emissions (per link) per meter

			String linkEmissionPerMOutputFile = config.controler().getOutputDirectory() + ".emissionsPerLinkPerM.csv";
			log.info("Writing emissions per link [g/m] to: {}", linkEmissionPerMOutputFile);
			File file1 = new File(linkEmissionPerMOutputFile);
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(file1));

			bw1.write("linkId");

			for ( Pollutant pollutant : Pollutant.values()) {
				bw1.write(";" + pollutant + " [g/m]");
			}
			bw1.newLine();

			Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsOnLinkEventHandler.getLink2pollutants();

			for (Id<Link> linkId : link2pollutants.keySet()) {
				bw1.write(linkId.toString());

				for (Pollutant pollutant : Pollutant.values()) {
					double emission = 0.;
					if (link2pollutants.get(linkId).get(pollutant) != null) {
						emission = link2pollutants.get(linkId).get(pollutant);
					}

					double emissionPerM = Double.NaN;
					Link link = scenario.getNetwork().getLinks().get(linkId);
					if (link != null) {
						emissionPerM = emission / link.getLength();
					}

					bw1.write(";" + emissionPerM);
				}
				bw1.newLine();
			}

			bw1.close();
			log.info("Output written to " + config.controler().getOutputDirectory());
			log.info("-------------------------------------------------");
		}

	}

}
