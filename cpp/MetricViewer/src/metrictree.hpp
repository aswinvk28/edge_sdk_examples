#ifndef METRIC_TREE_HPP
#define METRIC_TREE_HPP

#include <memory>
#include <set>
#include <string>

#include "metrics.hpp"
#include <com.adlinktech.edgesdk/MetricValueList.h>

namespace com {
namespace adlinktech {
namespace datariver {
namespace metrics {

using namespace com::adlinktech::edgesdk::v1_DOT_0;

class MetricTree {
public:
    struct Node {
        std::string label;
    };
    using NodePtr = std::unique_ptr<Node>;
    struct NodeCompare {
        bool operator()(const NodePtr& n1, const NodePtr& n2) {
            return n1->label.compare(n2->label);
        }
    };
    struct ValueNode : public Node {
        ValueNode(const std::string& label, const std::string& value) : Node{label}, value(value) {}
        ValueNode() = default;
        ValueNode(const ValueNode&) = default;
        ValueNode(ValueNode&&) = default;
        ValueNode& operator=(const ValueNode&) = default;
        ValueNode& operator=(ValueNode&&) = default;

        std::string value;
    };
    struct InteriorNode : public Node {
        InteriorNode(const std::string& label) : Node{label}, children() {}
        InteriorNode() = default;
        InteriorNode(const InteriorNode&) = default;
        InteriorNode(InteriorNode&&) = default;
        InteriorNode& operator=(const InteriorNode&) = default;
        InteriorNode& operator=(InteriorNode&&) = default;

        std::set<NodePtr, NodeCompare> children;
    };


    MetricTree() : root(std::unique_ptr<Node>(new InteriorNode{""})) {}
    MetricTree(const MetricTree &) = delete;
    MetricTree(MetricTree && other) : root(std::move(other.root)) {}
    MetricTree& operator=(const MetricTree &) = delete;
    MetricTree& operator=(MetricTree && other) { root = std::move(other.root); return *this; }

    void add(const MetricList& list) {
        for(const auto& metric: list.metrics()){
            MetricPath path{metric.metricId()};
            std::string leafLabel = path.filename();
            std::string leafValue = "TODO"; // (stringstream{} >> metric).str();
            path.remove_filename();
            InteriorNode * current = static_cast<InteriorNode*>(root.get());
            for(auto& label : path) {
                auto result = current->children.emplace(new InteriorNode{label});
                current = static_cast<InteriorNode*>((*result.first).get());
            }
            current->children.emplace(new ValueNode{leafLabel, leafValue});
        }
    }

    template<class Visitor>
    void accept(Visitor& visitor) {
        doAccept(visitor, static_cast<InteriorNode*>(root.get()));
    }

private:
    NodePtr root;

    template<class Visitor>
    void doAccept(Visitor& visitor, InteriorNode* parent) {

    }
};

} /* metrics */
} /* datariver */
} /* adlink */
} /* com */

#endif // METRIC_TREE_HPP
