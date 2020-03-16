package com.amithkoujalgi.auth.security;

import com.amithkoujalgi.auth.App;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultitenantConfigResolver implements KeycloakConfigResolver {

	private static AdapterConfig adapterConfig;

	private final Map<String, KeycloakDeployment> cache = new ConcurrentHashMap<String, KeycloakDeployment>();

	public static void setAdapterConfig( AdapterConfig adapterConfig )
	{
		MultitenantConfigResolver.adapterConfig = adapterConfig;
	}

	public String getDomainName( String url ) throws URISyntaxException
	{
		URI uri = new URI(url);
		String domain = uri.getHost();
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	@Override
	public KeycloakDeployment resolve( OIDCHttpFacade.Request request )
	{
		String url = null;
		try
		{
			url = getDomainName(request.getURI());
		}
		catch( URISyntaxException e )
		{
			e.printStackTrace();
		}
		String realm = url.split("\\.")[0];
		//		System.out.println("[Realm] - " + realm);
		KeycloakDeployment deployment = cache.get(realm);
		if( null == deployment )
		{
			// not found on the simple cache, try to load it from the file system
			try
			{
				FileInputStream is = new FileInputStream(
						App.REALMS_DIRECTORY + File.separator + realm + "-keycloak.json");
				if( is == null )
				{
					throw new IllegalStateException("Not able to find the file /" + realm + "-keycloak.json");
				}
				deployment = KeycloakDeploymentBuilder.build(is);
				cache.put(realm, deployment);
			}
			catch( FileNotFoundException e )
			{
				e.printStackTrace();
			}
		}

		return deployment;
	}

}