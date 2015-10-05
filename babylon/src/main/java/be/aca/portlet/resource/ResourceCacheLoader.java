package be.aca.portlet.resource;

import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.liferay.portal.kernel.util.StringPool;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import javax.portlet.ResourceRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import be.aca.portlet.resource.annotation.Context;
import be.aca.portlet.resource.annotation.Param;

public class ResourceCacheLoader extends CacheLoader<ResourceContext, String> {

	@Override
	public String load(ResourceContext context) throws Exception {
		Method method = context.getMethod();
		Annotation[][] paramAnnotations = method.getParameterAnnotations();
		// http://www.beyondjava.net/blog/reading-java-8-method-parameter-named-reflection/
		ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
		String[] names = parameterNameDiscoverer.getParameterNames(method);
		Class[] types = method.getParameterTypes();

		// http://stackoverflow.com/questions/15139424/not-able-to-invoke-main-method-using-reflection-illegalargumentexception-argu
		List<Object> values = Lists.<Object>newArrayList();
		for (int i = 0; i < paramAnnotations.length; i++) {
			// http://tutorials.jenkov.com/java-reflection/annotations.html#parameter
			String name = names[i];
			Class type = types[i];

			Annotation[] annotations = paramAnnotations[i];

			if (annotations.length > 0) {
				for (Annotation annotation : annotations) {
					// http://stackoverflow.com/questions/3348363/checking-if-an-annotation-is-of-a-specific-type
					if (annotation instanceof Param) {
						String value = context.getRequest().getParameter(name);
						values.add(value);
					}
					if (annotation instanceof Context) {
						if (type.isAssignableFrom(ResourceRequest.class)) {
							values.add(context.getRequest());
						} else {
							values.add(context.getResponse());
						}
					}
				}
			}
		}

		Object result = method.invoke(context.getPortlet(), values.toArray());

		String json = StringPool.BLANK;
		if (result != null) {
			Gson gson = new Gson();
			json = gson.toJson(result);
		}

		return json;
	}
}