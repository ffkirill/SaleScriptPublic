function ScenarioGraphView() {
    function intersectEllipse(node, rx, ry, point) {
        // Formulae from: http://mathworld.wolfram.com/Ellipse-LineIntersection.html

        var cx = node.x;
        var cy = node.y;

        var px = cx - point.x;
        var py = cy - point.y;

        var det = Math.sqrt(rx * rx * py * py + ry * ry * px * px);

        var dx = Math.abs(rx * ry * px / det);
        if (point.x < cx) {
            dx = -dx;
        }
        var dy = Math.abs(rx * ry * py / det);
        if (point.y < cy) {
            dy = -dy;
        }

        return {x: cx + dx, y: cy + dy};
    }

    function intersectRect(node, point) {
        var x = node.x;
        var y = node.y;

        // Rectangle intersection algorithm from:
        // http://math.stackexchange.com/questions/108113/find-edge-between-two-boxes
        var dx = point.x - x;
        var dy = point.y - y;
        var w = node.width / 2;
        var h = node.height / 2;

        var sx, sy;
        if (Math.abs(dy) * w > Math.abs(dx) * h) {
            // Intersection is top or bottom of rect.
            if (dy < 0) {
                h = -h;
            }
            sx = dy === 0 ? 0 : h * dx / dy;
            sy = h;
        } else {
            // Intersection is left or right of rect.
            if (dx < 0) {
                w = -w;
            }
            sx = w;
            sy = dx === 0 ? 0 : w * dy / dx;
        }

        return {x: x + sx, y: y + sy};
    }

    this.renderer = new dagreD3.render();
    this.renderer.shapes().start_end = function (parent, bbox, node) {
        var rx = bbox.width / 2,
            ry = bbox.height / 2,
            shapeSvg = parent.insert("ellipse", ":first-child")
                .attr("x", -bbox.width / 2)
                .attr("y", -bbox.height / 2)
                .attr("rx", rx)
                .attr("ry", ry);
        if (node.tabIndex) {
            shapeSvg = shapeSvg.attr().attr("tabindex", node.tabIndex);
        }


        node.intersect = function (point) {
            return intersectEllipse(node, rx, ry, point);
        };

        return shapeSvg;
    };

    this.renderer.shapes().question_reply = function (parent, bbox, node) {
        var shapeSvg = parent.insert("rect", ":first-child")
            .attr("rx", node.rx)
            .attr("ry", node.ry)
            .attr("x", -bbox.width / 2)
            .attr("y", -bbox.height / 2)
            .attr("width", bbox.width)
            .attr("height", bbox.height);

        if (node.tabIndex) {
            shapeSvg = shapeSvg.attr().attr("tabindex", node.tabIndex);
        }

        node.intersect = function(point) {
            return intersectRect(node, point);
        };

        return shapeSvg;
    };

    this.viewport = undefined;
    this.svg = undefined;
    this.selection = undefined;
    this.onNodeSelected = function (node) {
        console.log(node);
    };
    this.afterReRender = undefined;
}

ScenarioGraphView.wordwrap = function(text) {
    var start = 0, stop = 55, re = /(\S+\s+)/;
    var chunks = text.toString()
        .split(re)
        .reduce(function (acc, x) {
            acc.push(x);
            return acc;
        }, [])
        ;

    return chunks.reduce(function (lines, rawChunk) {
        if (rawChunk === '') return lines;

        var chunk = rawChunk.replace(/\t/g, '    ');

        var i = lines.length - 1;
        if (lines[i].length + chunk.length > stop) {
            lines[i] = lines[i].replace(/\s+$/, '');

            chunk.split(/\n/).forEach(function (c) {
                lines.push(
                    new Array(start + 1).join(' ')
                    + c.replace(/^\s+/, '')
                );
            });
        }
        else if (chunk.match(/\n/)) {
            var xs = chunk.split(/\n/);
            lines[i] += xs.shift();
            xs.forEach(function (c) {
                lines.push(
                    new Array(start + 1).join(' ')
                    + c.replace(/^\s+/, '')
                );
            });
        }
        else {
            lines[i] += chunk;
        }

        return lines;
    }, [ new Array(start + 1).join(' ') ]).join('\n');
};

ScenarioGraphView.prototype.initViewport = function(selector) {
    this.svg = d3.select(selector);
    this.viewport = this.svg.append("g");
};

ScenarioGraphView.prototype.processClick = function(e) {
    console.log(e);
};

ScenarioGraphView.prototype.draw = function(gg, select, scroll, highlight) {
    var g = new dagreD3.graphlib.Graph().setGraph({});
    var graphView = this;
    this.selection = select;

    var tabCount = 2;

    g.setNode(gg.entry.id, {
        label: gg.entry.text,
        id: gg.entry.id,
        kind: 'entry',
        shape: 'start_end',
        tabIndex: tabCount++
    });

    Object.keys(gg.phrases).forEach(function(key) {
        g.setNode(key, {
            id: key,
            kind: 'phrase',
            label: ScenarioGraphView.wordwrap(gg.phrases[key].text),
            shape: 'question_reply',
            labelType: 'text',
            style: highlight? 'fill: #4CD54C':'',
            tabIndex: tabCount++
        });
        var replies = gg.phrases[key].repliesList;
        for (var i=0; i<replies.length; ++i) {
            g.setNode(replies[i], {
                id: replies[i],
                kind: 'reply',
                label: ScenarioGraphView.wordwrap(gg.replies[replies[i]].text),
                shape: 'question_reply',
                labelType: 'text',
                tabIndex: tabCount++,
                rx: 8, ry: 8});
            g.setEdge(key, replies[i], {lineInterpolate: 'basis', id: key});
        }
    });

    g.setNode(gg.success.id, {
        label: gg.success.text,
        id: gg.success.id,
        kind: 'success',
        shape: 'start_end',
        tabIndex: tabCount++,
        style: highlight? 'fill: #4CD54C':''
    });

    Object.keys(gg.connections).forEach(function(key) {
        g.setEdge(key, gg.connections[key], {lineInterpolate: 'basis', id: key });
    });

    // Run the renderer. This is what draws the final graph.
    this.renderer(this.viewport, g);
    this.viewport.selectAll('g.selected').classed('selected', false);
    if (this.selection !== undefined) {
        this.viewport.selectAll('g[id="' + this.selection + '"]')
            .classed('selected', true)
            .each(function(){if (scroll) this.scrollIntoView()});
    }

    // Center the graph
    this.svg.attr("height", g.graph().height + 40);
    this.svg.attr("width", g.graph().width + 100);
    var xCenterOffset = (this.svg.attr("width") - g.graph().width) / 2;
    this.viewport.attr("transform", "translate(" + xCenterOffset + ", 20)");


    var viewport = this.viewport,
        cb = function(node) {
            this.selection = node !== undefined && node.id;
            this.onNodeSelected(node)
        }.bind(this);

    if (this.afterReRender !== undefined) {
        try {this.afterReRender()} catch (e) {}
        this.afterReRender = undefined;
    }

    //Setup on node click
    var onActivateCb = function() {
        var id = this.attributes.id.value,
            node = g.node(id),
            target;
        if ((d3.event.type == 'click' || d3.event.type == 'keydown')
            && graphView.selection != id) {
            graphView.afterReRender = function () {
                this.viewport.selectAll('g.selected [tabindex]').each(
                    function() {this.focus()}
                )
            }.bind(graphView);
        } else {
            try {d3.event.target.focus()} catch (e) {}
        }
        d3.event.stopPropagation();
        if (node.kind == 'entry') {
            target = gg.entry;
        } else if (node.kind == 'success') {
            target = gg.success;
        } else if (node.kind == 'phrase') {
            target = gg.phrases[id];
        } else if (node.kind == 'reply') {
            target = gg.replies[id];
        }
        cb(target);
    };

    this.viewport.selectAll("g.node").on("click", onActivateCb);
    this.viewport.selectAll("g.node").on("keydown", function (e) {
        if (d3.event.keyCode == 32) { //space
            onActivateCb.bind(this)();
        }
    });

};
