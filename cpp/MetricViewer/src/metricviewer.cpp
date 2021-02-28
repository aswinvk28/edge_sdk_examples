/*
 *                         ADLINK Edge SDK
 *
 *   This software and documentation are Copyright 2018 to 2020 ADLINK
 *   Technology Limited, its affiliated companies and licensors. All rights
 *   reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

#include <iostream>
#include <ostream>
#include <sstream>
#include <string>
#include <map>
#include <vector>
#include <stdlib.h>

#include <JSonThingAPI.hpp>
#include <com.adlinktech.edgesdk/MetricViewer.h>
#include <com.adlinktech.edgesdk/MetricList.h>
#include <com.adlinktech.edgesdk/MetricValueList.h>
#include <metrics.hpp>

#include "metrictree.hpp"

using namespace com::adlinktech::datariver;
using namespace com::adlinktech::datariver::metrics;
using namespace com::adlinktech::edgesdk::v1_DOT_0;

std::string getenv(std::string var_name) {
    char *env_var_ptr;

    #ifdef _WIN32
    #pragma warning( push )
    #pragma warning( disable : 4996 )
    #endif
    env_var_ptr = ::getenv(var_name.c_str());
    #ifdef _WIN32
    #pragma warning( pop )
    #endif

    return std::string(env_var_ptr ? env_var_ptr : "");
}

void print_usage()
{
    std::cout << "Usage: metricviewer (agents|metrics|display) [options]" << std::endl;
}

//Thing Class for built-in metric Thing agent
const std::string thingClassDefinition =
        R"(
    {
      "name": "MetricViewer",
      "context": "com.adlinktech.edgesdk",
      "version": "v1.0",
      "description": "EdgeSDK metric publisher agent thing class",
      "inputs": [
        {
          "name": "metrics",
          "tagGroupId": "MetricList:com.adlinktech.edgesdk:v1.0"
        },
        {
          "name": "values",
          "tagGroupId": "MetricValueList:com.adlinktech.edgesdk:v1.0"
        }
        ],
      "outputs": [
        {
          "name": "viewer",
          "tagGroupId": "MetricViewer:com.adlinktech.edgesdk:v1.0"
        }
        ]
    }
)";

std::string getMetricViewerThingDefinition(const std::vector<std::string>& agents)
{
    std::stringstream ss;
    ss << R"(
    {
        "id": "metricviewer_AUTO_",
        "classId": "MetricViewer:com.adlinktech.edgesdk:v1.0",
        "description": "EdgeSDK Metic Viewer",
        "contextId": "metric_viewer",
        "inputSettings" : [)";
    std::vector<std::string> inputs { "metrics","values" };
    const char *input_sep = "";
    for(auto & input : inputs) {
        ss << input_sep << std::endl << R"(
        {
          "name" : ")" << input << R"(",
          "filters" : {
            "sourceContextFilters": [)" << std::endl;
        const char *agent_sep = "";
        for(auto & agent : agents) {
            ss << agent_sep << '"' << agent << '"';
            agent_sep = ", ";
        }
        ss << "]" << std::endl << R"(
          }
        })";
        input_sep = ",";
    }
    ss << R"(
        ]
    })";

    return ss.str();
}

void registerEdgeSDKArtifacts(DataRiver dr)
{
    // Note: Don't need to register the Metrics tag groups. They are already registered (unless metrics are turned off.)
    // TODO: add tag group registration anyhow.

    std::string path = "file://" + std::string(getenv(std::string("EDGE_SDK_HOME"))) + "/taggroups/";
    JSonTagGroupRegistry tgr;

    tgr.registerTagGroupsFromURI(path + "MetricViewer.json");
    tgr.registerTagGroupsFromURI(path + "MetricInfoListTagGroup.json");
    tgr.registerTagGroupsFromURI(path + "MetricValueListTagGroup.json");
    dr.addTagGroupRegistry(tgr);

    JSonThingClassRegistry tcr;
    tcr.registerThingClassesFromString(thingClassDefinition);
    dr.addThingClassRegistry(tcr);
}

void expressAgentInterests(ThingEx viewerThing, const std::vector<std::string> & agents)
{
    MetricViewer viewer;
    viewer.set_viewerContextId(viewerThing.getContextId());
    for(auto& agent : agents) {
        viewerThing.write("viewer", agent, viewer);
    }
}

ThingEx createViewerThing(DataRiver dr, const std::vector<std::string> & agents)
{
    JSonThingProperties tp;
    tp.readPropertiesFromString(getMetricViewerThingDefinition(agents));
    ThingEx viewerThing = dr.createThing(tp);
    expressAgentInterests(viewerThing, agents);
    return viewerThing;
}

int process_agents_command(int argc, char *argv[])
{
    auto dr = DataRiver::getInstance();

//    registerEdgeSDKArtifacts(dr);


//    auto viewerThing = createViewerThing(dr);

    auto things = dr.getDiscoveredThingRegistry().findDiscoveredThings("*", "MetricPublisher:com.adlinktech.edgesdk:v1.0");

    for(const auto& thing : things) {
        std::cout << thing.getContextId() << std::endl;
    }

    // TODO: ensure this is always called, even if the code above throws.
    dr.close();

    return 0;
}

void issue_get_metrics_command(ThingEx viewerThing, const std::string& agentid)
{
    // do nothing!
}

int process_metrics_command(int argc, char *argv[])
{
    auto dr = DataRiver::getInstance();

    registerEdgeSDKArtifacts(dr);

    std::vector<std::string> agents;

    for(std::string agent; std::getline(std::cin, agent);) {
        agents.push_back(agent);
    }

    auto viewerThing = createViewerThing(dr, agents);

    std::size_t n_samples =  0;
    do {
        auto samples = viewerThing.read("metrics", 1000);
        n_samples = samples.size();
        for(const auto & sample: samples) {
            MetricList list;
            if(sample.getFlowState() == FlowState::ALIVE && sample.isCompatible(list)){
                sample.get(list);
                for(const auto& metric: list.metrics()){
                    // sample.getFlowId() is the agent context Id
                    std::cout << sample.getFlowId() << ":" << metric.metricId()
                            << std::endl;
                }
            }
        }

    } while(n_samples > 0);



    // TODO: ensure this is always called, even if the code above throws.
    viewerThing.close();
    dr.close();

    return 0;
}

using MetricsByAgent = std::map<std::string,std::vector<std::string>>;

void issue_metric_value_command(ThingEx viewerThing, const std::string agentid, const std::vector<std::string>& metricids)
{
    // nothing to do
}

void print_metrics(VLoanedDataSamples samples)
{
    for(const auto & sample: samples) {
        MetricValueList data;
        if(sample.getFlowState() == FlowState::ALIVE && sample.isCompatible(data)){
            sample.get(data);
            for(const auto& metric: data.values()){
                std::cout << metric.metricId() << ": ";
                auto mValue = deserialize_metric_value(metric.value());
                const auto& type = mValue.type();
                if(type == typeid(std::int64_t)){
                    std::cout << metric_cast<std::int64_t>(mValue);
                }else if(type == typeid(const std::vector<std::string>&)){
                    auto values = metric_cast<std::vector<std::string>>(mValue);
                    const char *sep = "";
                    for(auto& val: values){
                        std::cout << sep << val;
                        sep = ", ";
                    }
                }else if(type == typeid(const std::string&)){
                    std::cout << metric_cast<std::string>(mValue);
                } else {
                    std::cout << "<unhandled type>";
                }
                std::cout << std::endl;
            }
        }
    }
}

void metric_value_to_stream(const MetricValue& mValue, std::ostream& os)
{
    const auto& type = mValue.type();
    if(type == typeid(std::int64_t)){
        os << metric_cast<std::int64_t>(mValue);
    }else if(type == typeid(const std::vector<std::string>&)){
        auto values = metric_cast<std::vector<std::string>>(mValue);
        const char *sep = "";
        for(auto& val: values){
            os << sep << val;
            sep = ", ";
        }
    }else if(type == typeid(const std::string&)){
        os << metric_cast<std::string>(mValue);
    } else {
        os << "<unhandled type>";
    }

}

struct TreeValue {
    int level;
    std::string value;
};

void print_metrics_in_tree(VLoanedDataSamples samples)
{
    std::map<std::string,TreeValue> valuesTree;

    for(const auto & sample: samples) {
        MetricValueList data;
        if(sample.getFlowState() == FlowState::ALIVE && sample.isCompatible(data)){
            sample.get(data);
            for(const auto& metric: data.values()){
                MetricPath mp(metric.metricId());
                int level = 0;
                for(auto it = mp.begin(); it != mp.end(); ++it, ++level) {
                    auto next = it;
                    if(++next == mp.end()) {
                        //leaf not, get the value
                        std::stringstream ss;
                        metric_value_to_stream(deserialize_metric_value(metric.value()), ss);
                        valuesTree[*it] = TreeValue{level, ss.str()};
                    } else {
                        valuesTree[*it] = TreeValue{level, ""};
                    }
                }
            }
        }
    }

    for(auto& tv : valuesTree) {
        for(int i = 0; i < tv.second.level; i++) {
            std::cout << "  ";
        }
        std::cout << tv.first;
        if(!tv.second.value.empty()) {
            std::cout << ": " << tv.second.value;
        }
        std::cout << std::endl;
    }
}

void read_and_print_metrics(ThingEx viewerThing)
{
    std::size_t n_samples =  0;
    do {
        auto samples = viewerThing.read("values", 5000);
        n_samples = samples.size();
//        print_metrics_in_tree(samples);
        print_metrics(samples);

    } while(n_samples > 0);

}

void issue_metric_value_commands(ThingEx viewerThing, const MetricsByAgent & metrics_by_agent)
{
    // nothingg to do
}

int process_display_command(int argc, char *argv[])
{
    auto dr = DataRiver::getInstance();

    registerEdgeSDKArtifacts(dr);

    std::vector<std::string> agents;

    for(std::string agent; std::getline(std::cin, agent);) {
        agents.push_back(agent);
    }

    auto viewerThing = createViewerThing(dr, agents);

    read_and_print_metrics(viewerThing);

    // TODO: ensure this is always called, even if the code above throws.
    viewerThing.close();
    dr.close();

    return 0;
}

int main(int argc, char *argv[]) {
    if(argc <= 1) {
        print_usage();
        return 1;
    }
    std::string command = argv[1];
    try {
        if(command == "agents") {
            return process_agents_command(argc, argv);
        }
        if(command == "metrics") {
            return process_metrics_command(argc, argv);
        }
        if(command == "display") {
            return process_display_command(argc, argv);
        }
    } catch(...) {
        std::cerr << "Unexpected exception occurred. Exiting." << std::endl;
        return 1;
    }
    return 0;
}
