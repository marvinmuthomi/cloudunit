/*
 * LICENCE : CloudUnit is available under the GNU Affero General Public License : https://gnu.org/licenses/agpl.html
 *     but CloudUnit is licensed too under a standard commercial license.
 *     Please contact our sales team if you would like to discuss the specifics of our Enterprise license.
 *     If you are not sure whether the GPL is right for you,
 *     you can always test our software under the GPL and inspect the source code before you contact us
 *     about purchasing a commercial license.
 *
 *     LEGAL TERMS : "CloudUnit" is a registered trademark of Treeptik and can't be used to endorse
 *     or promote products derived from this project without prior written permission from Treeptik.
 *     Products or services derived from this software may not be called "CloudUnit"
 *     nor may "Treeptik" or similar confusing terms appear in their names without prior written permission.
 *     For any questions, contact us : contact@treeptik.fr
 */

package fr.treeptik.cloudunit.cli.utils;

import static java.lang.System.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.treeptik.cloudunit.cli.model.ModuleAndContainer;
import fr.treeptik.cloudunit.cli.model.ServerAndContainer;
import fr.treeptik.cloudunit.dto.AboutResource;
import fr.treeptik.cloudunit.dto.AliasResource;
import fr.treeptik.cloudunit.dto.ApplicationResource;
import fr.treeptik.cloudunit.dto.Command;
import fr.treeptik.cloudunit.dto.ContainerUnit;
import fr.treeptik.cloudunit.dto.EnvironmentVariableResource;
import fr.treeptik.cloudunit.dto.FileUnit;
import fr.treeptik.cloudunit.dto.ModulePortResource;
import fr.treeptik.cloudunit.dto.ModuleResource;
import fr.treeptik.cloudunit.model.Application;
import fr.treeptik.cloudunit.model.Image;
import fr.treeptik.cloudunit.model.Message;
import fr.treeptik.cloudunit.model.Snapshot;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.model.Volume;

public class MessageConverter {
    private static ShellRowFormater printer = new ShellRowFormater(out);
    private static Logger logger = Logger.getLogger("MessageConverter");

    public static void buildApplicationMessage(ApplicationResource application, ServerAndContainer server,
            Collection<ModuleAndContainer> modules) {
        logger.log(Level.WARNING, "\n GENERAL \n");

        printer.print(new String[][] {
                new String[] { "APPLICATION NAME", "AUTHOR", "STARTING DATE", "SERVER TYPE", "STATUS" },
                new String[] {
                        application.getName(),
                        application.getUserDisplayName(),
                        DateUtils.formatDate(application.getCreationDate()),
                        application.getServerType(),
                        application.getStatus().toString()
                        },

        });

        logger.log(Level.WARNING, "\n SERVER INFORMATION \n");

        buildServerMessage(server);
        buildModuleMessage(modules);
    }

    public static void buildServerMessage(ServerAndContainer server) {
        String[][] tab = new String[][] {
            new String[] {
                    "TYPE",
                    "SSH PORT",
                    "STATUS",
                    "JVM OPTS",
                    "MEMORY",
                    "MANAGER LOCATION",
            },
            new String[] {
                    server.server.getImage().getName(),
                    server.container.getSshPort(),
                    server.container.getStatus().toString(),
                    server.server.getJvmOptions() != "" ? server.server.getJvmOptions() : "NONE",
                    server.server.getJvmMemory().toString(),
                    server.server.getLink("manager").getHref(),
            },
        };

        printer.print(tab);
    }

    public static void buildImageResponse(Image image) {
        String status = "";
        if (image.getStatus().equals(0)) {
            status = "DISABLED";
        } else {
            status = "ENABLED";
        }

        printer.print(
                new String[][] { new String[] { "IMAGE NAME", "NEW STATUS" }, new String[] { image.getName(), status },

                });
    }

    public static void buildImageListResponse(List<Image> images) {
        String[][] tab = new String[images.size() + 1][2];
        tab[0][0] = " IMAGE NAME";
        tab[0][1] = "STATUS";
        int i = 0;
        for (Image image : images) {
            String status = image.getStatus().equals(0) ? status = "DISABLED" : "ENABLED";
            tab[i + 1][0] = image.getName();
            tab[i + 1][1] = status;
            i++;
        }
        printer.print(tab);
    }

    public static void buildModuleMessage(Collection<ModuleAndContainer> modules) {
        List<String[]> tabs = new ArrayList<>();
        
        tabs.add(new String[] { "MODULE NAME", "DOMAIN NAME", "PORTS", });
        
        for (ModuleAndContainer module : modules) {
            tabs.add(new String[] {
                    module.module.getName(),
                    module.container.getInternalDnsName(),
                    String.join(" ", module.ports.stream().map(p -> buildPortMessage(p)).toArray(String[]::new)),
            });
        }
        
        printer.print(tabs.toArray(new String[0][]));
    }
    
    public static String buildPortMessage(ModulePortResource port) {
        return String.format("%s->%s",
                port.getNumber(),
                port.getHostNumber() != null ? port.getHostNumber() : "x");
    }

    public static void buildLightModuleMessage(Collection<ModuleResource> modules) {
        printer.print(new String[][] { modules.stream().map(m -> m.getName()).toArray(String[]::new) });
    }

    public static void buildListApplications(Collection<ApplicationResource> appications) {
        List<String[]> tab = new ArrayList<>();
        
        tab.add(new String[] {
                "APPLICATION NAME",
                "AUTHOR",
                "STARTING DATE",
                "SERVER TYPE",
                "STATUS",
        });
        
        for (ApplicationResource application : appications) {
            tab.add(new String [] {
                    application.getName(),
                    application.getUserDisplayName(),
                    DateUtils.formatDate(application.getCreationDate()),
                    application.getServerType(),
                    application.getStatus().name(),
            });
        }
        printer.print(tab.toArray(new String[0][]));
    }

    public static void buildListUsers(List<User> users) {

        if (users.isEmpty()) {
            logger.log(Level.WARNING, "No apps found!");

        } else {

            String[][] tab = new String[users.size() + 1][6];
            tab[0][0] = "LOGIN";
            tab[0][1] = "FIRSTNAME";
            tab[0][2] = "LASTNAME";
            tab[0][3] = "EMAIL";
            tab[0][4] = "LAST CONNECTION";
            tab[0][5] = "STATUS";

            User user = null;
            for (int i = 0; i < users.size(); i++) {
                user = users.get(i);
                tab[i + 1][0] = user.getLogin();
                tab[i + 1][1] = user.getFirstName();
                tab[i + 1][2] = user.getLastName();
                tab[i + 1][3] = user.getEmail();
                tab[i + 1][4] = user.getLastConnection() != null ? DateUtils.formatDate(user.getLastConnection())
                        : "NEVER";
                tab[i + 1][5] = user.getRole().getDescription().substring(5);
            }
            printer.print(tab);
        }
    }

    public static void buildListSnapshots(List<Snapshot> snapshots) {

        if (snapshots.isEmpty()) {
            logger.log(Level.WARNING, "No snapshots found!");

        } else {

            String[][] tab = new String[snapshots.size() + 1][3];
            tab[0][0] = "TAG";
            tab[0][1] = "DATE";
            tab[0][2] = "APPLICATION SOURCE";

            Snapshot snapshot = null;
            for (int i = 0; i < snapshots.size(); i++) {
                snapshot = snapshots.get(i);
                tab[i + 1][0] = snapshot.getTag();
                tab[i + 1][1] = DateUtils.formatDate(snapshot.getDate());
                tab[i + 1][2] = snapshot.getApplicationName();

            }
            printer.print(tab);
        }
    }

    public static void buildUserMessages(List<Message> messages) {

        String[][] tab = new String[messages.size() + 1][4];
        tab[0][0] = "USER";
        tab[0][1] = "TYPE";
        tab[0][2] = "DATE";
        tab[0][3] = "EVENT";

        Message message = null;
        for (int i = 0; i < messages.size(); i++) {
            message = messages.get(i);
            tab[i + 1][0] = message.getAuthor().getFirstName() + " " + message.getAuthor().getLastName();
            tab[i + 1][1] = message.getType();
            tab[i + 1][2] = DateUtils.formatDate(message.getDate());
            tab[i + 1][3] = message.getEvent();

        }
        printer.print(tab);

    }

    public static String buildListTags(List<String> tags) {
        StringBuilder builder = new StringBuilder();
        if (tags.isEmpty()) {
            return "No tag found!";
        }
        for (String tag : tags) {
            builder.append(tags.indexOf(tag) + " - ").append(tag + "\n");
        }
        return builder.toString();
    }

    public static void buildListContainerUnits(List<ContainerUnit> containerUnits, String string,
            Application application) {
        logger.log(Level.INFO, "Available containers for application : " + application.getName());
        String[][] tab = new String[containerUnits.size() + 1][2];
        tab[0][0] = "CONTAINER NAME";
        tab[0][1] = "TYPE";

        ContainerUnit containerUnit = null;
        for (int i = 0; i < containerUnits.size(); i++) {
            containerUnit = containerUnits.get(i);
            tab[i + 1][0] = containerUnit.getName().substring((application.getUser().getFirstName()
                    + application.getUser().getLastName() + "-" + application.getName() + "-").length());
            tab[i + 1][1] = containerUnit.getType();
        }
        printer.print(tab);

    }

    public static void buildListAliases(Collection<AliasResource> aliases) {
        printer.print(new String[][] { aliases.stream().map(a -> a.getName()).toArray(String[]::new) });
    }

    public static void buildListEnvironmentVariables(Collection<EnvironmentVariableResource> environmentVariables) {
        List<String[]> tab = new ArrayList<>();
        tab.add(new String[] {
                "ENVIRONMENT VARIABLE",
                "VALUE",
        });

        for (EnvironmentVariableResource variable : environmentVariables) {
            tab.add(new String[] {
                    variable.getName(),
                    variable.getValue(),
            });
        }
        printer.print(tab.toArray(new String[0][]));
    }

    public static String buildListVolumes(List<Volume> volumes) {
        StringBuilder builder = new StringBuilder(512);
        String[][] tab = new String[volumes.size() + 1][1];
        tab[0][0] = "VOLUMES NAMES";
        if (volumes.size() == 0) {
            logger.log(Level.INFO, "It has not custom volume");
        } else {
            for (int i = 0; i < volumes.size(); i++) {
                tab[i + 1][0] = volumes.get(i).getName();
                builder.append(volumes.get(i).getName()).append(":");
            }
            printer.print(tab);
        }
        logger.log(Level.INFO, builder.toString());
        return builder.toString();
    }

    public static String buildListFileUnit(List<FileUnit> fileUnits) {
        StringBuilder builder = new StringBuilder(512);
        for (FileUnit fileUnit : fileUnits) {
            if (fileUnit.getName().equalsIgnoreCase(".")) {
                continue;
            }
            builder.append("\t" + fileUnit.getName() + "\t");
        }
        logger.log(Level.INFO, builder.toString());
        return builder.toString();
    }

    public static void buildListCommands(List<Command> commands) {
        String[][] tab = new String[commands.size() + 1][3];
        tab[0][0] = "CURRENT COMMAND";
        tab[0][1] = "ARGUMENT NUMBER REQUIRED";
        tab[0][2] = "ARGUMENTS";

        if (commands.size() == 0) {
            logger.log(Level.INFO, "This application has not custom command");
        } else {
            for (int i = 0; i < commands.size(); i++) {
                tab[i + 1][0] = commands.get(i).getName();
                tab[i + 1][1] = commands.get(i).getArgumentNumber().toString();
                String arguments = "";
                for (String argument : commands.get(i).getArguments())
                    arguments = arguments + argument + " ";
                arguments = arguments.substring(0, arguments.length() - 1);
                tab[i + 1][2] = arguments;
            }
            printer.print(tab);
        }
    }

    public static void buildListContainers(List<String> containers) {
        String[][] tab = new String[containers.size() + 1][1];
        tab[0][0] = "CONTAINER NAME";

        if (containers.size() == 0) {
            logger.log(Level.INFO, "This application has not container");
        } else {
            for (int i = 0; i < containers.size(); i++) {
                tab[i + 1][0] = containers.get(i);
            }
            printer.print(tab);
        }
    }
    
    public static String buildAbout(String version, String timestamp) {
        return String.format("CloudUnit CLI version %s (build timestamp %s)",
                version,
                timestamp);
    }

    public static String buildAbout(String version, String timestamp, AboutResource aboutApi) {
        return String.format("%s%nCloudUnit Manager API version %s (build timestamp %s)",
                buildAbout(version, timestamp),
                aboutApi.getVersion(),
                aboutApi.getTimestamp());
    }

}
