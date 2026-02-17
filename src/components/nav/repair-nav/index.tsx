"use client"
import { Button } from "@/components/ui/button"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { History, Wrench } from "lucide-react";

export default function RepairNav() {

    //const [repairFormEnabled, setRepairFormEnabled]  = useState(false);

    function createRepairOrder(): any {
        // setRepairFormEnabled(!repairFormEnabled);
        console.log("Loading...");
    }

    return (
        <>
            <Tabs defaultValue="repairOrders">
                <TabsList>
                    <TabsTrigger value="repairOrders">
                    <Wrench />
                    Repair Orders
                    </TabsTrigger>
                    <TabsTrigger value="history">
                    <History />
                    History
                    </TabsTrigger>
                </TabsList>
                <Button variant={"outline"} onClick={createRepairOrder}>Create</Button>
            </Tabs>
        
        </>
    )
}