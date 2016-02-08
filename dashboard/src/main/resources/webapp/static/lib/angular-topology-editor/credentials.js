/**
 * This is a proof of concept of topology editor using Graph.Node objects.
 *
 *
 * The following prefixes in variables are used:
 *   j_ : jquery selections
 *   d3_ : d3 selections
 *   g_ : Graph.* nodes (or children)
 *
 * @author roman.sosa@atos.net
 */

 "use strict";

var Credentials = (function() {

    var activeform = undefined;
    var amazonform = Object.create(Forms.Form);
    var userpwdform = Object.create(Forms.Form);
    var cloudfoundryform = Object.create(Forms.Form);
    var genericform = Object.create(Forms.Form);

    var form_by_provider = {
        "aws-ec2": amazonform,
        "openshift": userpwdform,
        "CloudFoundry": cloudfoundryform,
    };

    var canvas;

    /*
     * Override some Link and Node methods regarding Canvas.
     */

    var LinkBehaviour = {
        popovercontent : function(i) {
        }
    };

    var NodeBehaviour = {

        popovertitle: function(i) {
            var result = this.name;
            if (this.type == "Cloud") {
                result += "&nbsp;&nbsp;&nbsp;" +
                    '<button type="button" class="popover-edit" data-action="edit" data-nodeindex="' + i + '">' +
                    '<span aria-hidden="true" class="fa fa-edit"></span></button>';
            }
            return result;
        },

        popovercontent: function(i) {

            var terms = [
                [ "Type", this.type],
                [ "Status", this.properties.status],
            ];

            var content = "";
            for (var i = 0; i < terms.length; i++) {
                var key = terms[i][0];
                var value = terms[i][1];
                content += "<dt>" + key + "</dt><dd>" + value + "</dd>";
            }
            return "<dl>" + content + "</dl>";
        }
    };


    function init(canvas) {
        this.canvas = canvas;

        initialize_fieldssets();
        initialize_forms();

        /*
         * Edit cloud
         */
        $('body').off('click', '.popover button[data-nodeindex]');
        $('body').on('click', '.popover button[data-nodeindex]', function () {
            var index = this.getAttribute("data-nodeindex");

            if (index === undefined) {
                return;
            };
            var node = index !== undefined? canvas.getnode(index) : undefined;
            var action = this.getAttribute("data-action");

            log.debug("Popover button click: action=" + action +
                " nodeid=" + index +
                " node=" + node.toString());

            /*
             * hide all popovers
             */
            $('[data-popover]').popover('hide');

            if (action == "edit") {
                /*
                 * and load form
                 */
                activeform = form_by_provider[node.properties.location] || genericform;
                activeform.title = node.name;
                activeform.reset();
                activeform.load(node);
                activeform.show(function(node) {
                    canvas.restart();
                    canvas.firechange();
                });
            }
        });

        /*
         * Edit cloud button in form
         */
        $("div.modal-footer button.btn-primary").on("click", function() {
            activeform.hide();
            activeform.createnode();
            activeform = undefined;
        });
    }

    var credentialset = Object.create(Forms.Fieldset);
    var amazonset = Object.create(credentialset);
    var userpwdset = Object.create(credentialset);
    var cloudfoundryset = Object.create(credentialset);
    var genericset = Object.create(credentialset);

    function initialize_fieldssets() {
        amazonset.credentialset("set-amazon");
        userpwdset.credentialset("set-userpwd");
        cloudfoundryset.credentialset("set-cloudfoundry");
        genericset.credentialset("set-generic");
    }

    function initialize_forms() {
        amazonform.setup(
            Types.Cloud,
            NodeBehaviour,
            document.getElementById("edit-credentials-form"),
            "Cloud",
            [amazonset]
        );
        userpwdform.setup(
            Types.Cloud,
            NodeBehaviour,
            document.getElementById("edit-credentials-form"),
            "Cloud",
            [userpwdset]
        );
        cloudfoundryform.setup(
            Types.Cloud,
            NodeBehaviour,
            document.getElementById("edit-credentials-form"),
            "Cloud",
            [userpwdset, cloudfoundryset]
        );
        genericform.setup(
            Types.Cloud,
            NodeBehaviour,
            document.getElementById("edit-credentials-form"),
            "Cloud",
            [genericset]
        );
    }

    credentialset.credentialset = function(fieldsetid) {
        this.setup(fieldsetid);
    }

    amazonset.load = function(node) {
        $("#amazon-identity").val(node.properties.identity);
        $("#amazon-credential").val(node.properties.credential);
    }

    amazonset.store = function(node) {
        node.properties.identity = $("#amazon-identity").val();
        node.properties.credential = $("#amazon-credential").val();
    }

    userpwdset.load = function(node) {
        $("#userpwd-user").val(node.properties.user);
        $("#userpwd-pwd").val(node.properties.password);
    }

    userpwdset.store = function(node) {
        node.properties.user = $("#userpwd-user").val();
        node.properties.password = $("#userpwd-pwd").val();
    }

    cloudfoundryset.load = function(node) {
        $("#cloudfoundry-org").val(node.properties.org);
        $("#cloudfoundry-space").val(node.properties.space);
        $("#cloudfoundry-address").val(node.properties.address);
        $("#cloudfoundry-endpoint").val(node.properties.endpoint);
    }

    cloudfoundryset.store = function(node) {
        node.properties.org = $("#cloudfoundry-org").val();
        node.properties.space = $("#cloudfoundry-space").val();
        node.properties.address = $("#cloudfoundry-address").val();
        node.properties.endpoint = $("#cloudfoundry-endpoint").val();
    }

    genericset.load = function(node) {
        var arr = []

        Object.keys(node.properties).
            forEach(function(name) {
                if (name != "location") {
                    arr.push(name + "=" + node.properties[name]);
                }
            });
        var s = arr.join(";");
        $("#generic-value").val(s);
    }

    genericset.store = function(node) {
        var arr = $("#generic-value").val().split(";");
        arr.forEach(function(item) {
            var parts = item.split("=");
            var name = parts[0];
            var value = parts[1];
            node.properties[name] = value;
        });
    }

    function addlinkcallback(canvas, link, accept) {
        return undefined;
    }

    function fromjson(json) {
        /*
         * Autogenerate typemap
         */
        var typemap = {};
        for (var i in Types) {
            typemap[i] =  Types[i];
        }

        for (var i = 0; i < json.nodes.length; i++) {
            json.nodes[i].behaviour = NodeBehaviour;
        }

        for (var i = 0; i < json.links.length; i++) {
            json.links[i].behaviour = LinkBehaviour;
        }

        this.canvas.fromjson(json, typemap);
    };

    /**
     * Returns a topology from a DAM.
     */
    function to_topology(rawdam) {
        var PAAS_TYPE_PREFIX = "seaclouds.nodes.Platform";
        var IAAS_TYPE_PREFIX = "seaclouds.nodes.Compute";
        var INSTANCES = "instancesPOC";
        var dam = jsyaml.safeLoad(rawdam);

        function node_type_is_provider(node_type_name) {
            return node_type_name.startsWith(PAAS_TYPE_PREFIX) ||
                node_type_name.startsWith(IAAS_TYPE_PREFIX);
        }

        function topology_type_by_node_template(node_template) {
            var result = undefined;
            if (node_template.type) {
                if (node_type_is_provider(node_template.type)) {
                    result = Types.Cloud;
                }
                else {
                    result = Types.Module;
                }
            }
            else {
                throw "Node Template does not have type";
            }
            return result;
        }

        function requirement_is_hosted_on_provider(requirement) {
            if (requirement.host) {
                var host_template = dam.topology_template.node_templates[requirement.host];
                if (host_template && node_type_is_provider(host_template.type)) {
                        return true;
                }
            }
            return false;
        }

        var topology = {
            "nodes": [],
            "links": []
        };

        var nodemap = {};
        /*
         * Create topology nodes
         */
        Object.keys(dam.topology_template.node_templates).forEach(function(name) {
            var node_template = dam.topology_template.node_templates[name];
            var ttype = topology_type_by_node_template(node_template);

            var tnode = Object.create(ttype).init({
                name: name,
                label: name,
                behaviour: Credentials.NodeBehaviour
            });

            if (ttype == Types.Cloud) {
                tnode.properties.location = node_template.properties.location;
            }
            topology.nodes.push(tnode);
            nodemap[name] = tnode;
        });
        /*
         * Create topology links
         */
        Object.keys(dam.topology_template.node_templates).forEach(function(name) {
            var node_template = dam.topology_template.node_templates[name];

            (node_template.requirements || []).
                filter(requirement_is_hosted_on_provider).
                forEach(function(requirement) {
                    var tmodule = nodemap[name];
                    var tcloud = nodemap[requirement.host];
                    var tlink = {
                        source: tmodule.name,
                        target: tcloud.name
                    };
                    topology.links.push(tlink);
                });
        });
        return topology;
    }

    /**
     * Injects the credentials currently stored in the canvas into the DAM
     * passed as parameter.
     * Returns the modified DAM.
     *
     * Assumes there is a group LOCATION_GROUP_<nodename> where nodename is the
     * name of a node template in the topology_template.
     */
    function store_credentials_in_dam(rawdam) {
        var LOCATION_GROUP = "add_brooklyn_location_";
        var LOCATION_POLICY = "brooklyn.location";

        var dam = jsyaml.safeLoad(rawdam);
        var canvas = this.canvas;
        Object.keys(dam.topology_template.groups).
            filter(function(groupname) {
                return groupname.startsWith(LOCATION_GROUP);
            }).
            forEach(function(groupname) {
                var group = dam.topology_template.groups[groupname];
                var tnodename = groupname.substr(LOCATION_GROUP.length);
                if (group.policies) {
                    for (var i = 0; i < group.policies.length; i++) {
                        var policy = group.policies[i];
                        if (policy[LOCATION_POLICY]) {
                            var tnode = canvas.getnodebyname(tnodename);

                            Object.keys(tnode.properties).
                            filter(function(name) {
                                name != "location"
                            }).
                            forEach(function(name) {
                                var value = tnode.properties[name];
                                policy[name] = value;
                            });
                            break;
                        }
                    }
                }
            });
        var rawDam = jsyaml.safeDump(dam);
        return rawDam;
    }

    return {
        init : init,
        addlinkcallback: addlinkcallback,
        fromjson: fromjson,
        to_topology: to_topology,
        store_credentials_in_dam: store_credentials_in_dam,
        canvas: canvas,
        NodeBehaviour: NodeBehaviour,
        LinkBehaviour: LinkBehaviour,
    };
})();
