/*
 * Copyright (C) 2022 Academy of Sciences Library
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.bankid_registrator;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@SpringBootApplication(
    scanBasePackages={"cz.cas.lib"},
    exclude={
        SecurityAutoConfiguration.class, 
        DataSourceAutoConfiguration.class, 
        org.springframework.boot.autoconfigure.data.ldap.LdapRepositoriesAutoConfiguration.class
    }
)
@PropertySources({
    @PropertySource(value="classpath:config.properties", ignoreResourceNotFound=false),
    // @PropertySource(value="file://${HOME}/.bankid-registrator/application.properties", ignoreResourceNotFound=true)
})
public class BankIDRegistratorApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(BankIDRegistratorApplication.class);
    }

    public static void main(String[] args) {

        new SpringApplicationBuilder(BankIDRegistratorApplication.class)
                .bannerMode(Banner.Mode.LOG)
                .properties("server.servlet.context-path=/bankid-registrator")
                .logStartupInfo(Boolean.FALSE)
                .build()
                .run(args);

    }

}
