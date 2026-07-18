package io.github.njw3995.fabricmmo.core.info;

import io.github.njw3995.fabricmmo.core.command.UpstreamCommandCatalog;import io.github.njw3995.fabricmmo.core.command.UpstreamCommandDefinition;import java.util.List;
public final class CommandHelpCatalog { private CommandHelpCatalog(){} public static List<UpstreamCommandDefinition> page(int page,int size){if(page<1||size<1)return List.of();List<UpstreamCommandDefinition> all=UpstreamCommandCatalog.instance().commands();int start=(page-1)*size;if(start>=all.size())return List.of();return all.subList(start,Math.min(start+size,all.size()));}public static int pages(int size){return Math.max(1,(UpstreamCommandCatalog.instance().commands().size()+size-1)/size);} }
