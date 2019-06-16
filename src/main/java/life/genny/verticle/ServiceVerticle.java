package life.genny.verticle;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;

import life.genny.channel.Routers;
import life.genny.channels.EBCHandlers;
import life.genny.cluster.Cluster;
import life.genny.eventbus.EventBusInterface;
import life.genny.eventbus.EventBusVertx;
import life.genny.eventbus.VertxCache;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwandautils.GennyCacheInterface;
import life.genny.qwandautils.GennySettings;
import life.genny.rules.RulesLoader;

import life.genny.utils.VertxUtils;

public class ServiceVerticle extends AbstractVerticle {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	  public void start() {

	    log.info("Loading initial Rules");
	    final Future<Void> startFuture = Future.future();
	    Cluster.joinCluster().compose(res -> {
	      final Future<Void> fut = Future.future();
	      EventBusInterface eventBus = new EventBusVertx();
	      GennyCacheInterface vertxCache = new VertxCache();
	      VertxUtils.init(eventBus,vertxCache);
	       loadInitialRules(GennySettings.rulesDir).compose(p -> {
	  
	     	    final Future<Void> startupfut = Future.future();
	     	    String disableRules = System.getenv("DISABLE_INIT_RULES_STARTUP");
	     	    if (!"TRUE".equalsIgnoreCase(disableRules)) {
	     	    	triggerStartupRules(GennySettings.rulesDir, eventBus).compose(q -> {
	     		    	  if (GennySettings.isRulesManager) {
	     		     		  Routers.routers(vertx);
	     		    		  Routers.activate(vertx);
	     		    	  }
	     		        
	     		        EBCHandlers.registerHandlers(eventBus);
	     		        
	     		       vertx.eventBus().consumer("health", (message) -> {
	     		            log.info("HEALTH CHECK RX"/*message.body().toString().length()*/);
	     		        });

	     	    		startupfut.complete();
	     	    	}, startupfut);
	     	    }
	     	   else {
	    	    	log.warn("DISABLE_INIT_RULES_STARTUP IS TRUE -> No Init Rules triggered.");
	    	    }
	        

	        fut.complete();
	        log.info("Rulesservice started");
	      }, fut);
	       
	  
	    }, startFuture);
	   

	  }
	  

	/**
	 * @param vertx
	 * @return
	 */
	public Future<Void> loadInitialRules(final String rulesDir) {

		final Future<Void> fut = Future.future();
		Vertx.currentContext().owner().executeBlocking(exec -> {

			log.info("Load Rules using Vertx 1");
			RulesLoader.loadRules(rulesDir);
			log.info("Load Rules using Vertx 2");
			fut.complete();
		}, failed -> {
		});

		return fut;
	}

	/**
	 * @param vertx
	 * @return
	 */
	public Future<Void> triggerStartupRules(final String rulesDir, EventBusInterface eventBus) {
		log.info("Triggering Startup Rules for all realms");
		final Future<Void> fut = Future.future();
		Vertx.currentContext().owner().executeBlocking(exec -> {// Force Genny first
			log.info("---- Realm:genny Startup Rules ----------");
			RulesLoader.triggerStartupRules(rulesDir, eventBus);
			fut.complete();
		}, failed -> {
		});

		return fut;
	}

}
