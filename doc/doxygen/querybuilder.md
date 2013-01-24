Create new query builder  {#querybuilder}
========================

A query builder is a class that
1. implements the [QueryBuilderPlugin interface](@ref QueryBuilderPlugin.java)
2. has the `@PluginImplementation` annotation

When implementing the interface you have to provide a short name, a caption
and a callback function that creates new Vaadin components. You get an object
of the type [ControlPanel](@ref annis.gui.controlpanel.ControlPanel) which you can use to set new queries from your
component.

A query builder plugin must be either registered in the @ref annis.gui.MainApp#initPlugins() function
of the [MainApp class](@ref annis.gui.MainApp) (if the plugin is part of the annis-gui project) or must be
added to a jar that is located at one of the following locations:
- the plugins folder inside the deployed web application
- the path defined in the `ANNIS_PLUGINS` environment variable

Please also note that it is possible to configure a default query builder for an
instance. Further information can be found in the adminstration guide.
