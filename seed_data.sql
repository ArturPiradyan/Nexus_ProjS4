-- File: seed_data.sql
-- Description: Sample campus data for Smart Campus Nexus.

USE SmartCampusNexus;
GO

-- Insert nodes
INSERT INTO MapNodes (NodeID, RoomName, FloorLevel, IsAccessible) VALUES
('ENTRANCE_A', 'Main Entrance', 0, 1),
('HALLWAY_A', 'Main Hallway Ground', 0, 1),
('LAB_302', 'Computer Lab 302', 0, 1),
('LIBRARY_2F', 'Library Second Floor', 1, 0),
('CANTEEN', 'Campus Canteen', 0, 1);

-- Insert edges for bidirectional paths
INSERT INTO MapEdges (SourceNode, TargetNode, BaseDistance, IsStairs) VALUES
('ENTRANCE_A', 'HALLWAY_A', 10, 0),
('HALLWAY_A', 'ENTRANCE_A', 10, 0),
('HALLWAY_A', 'LAB_302', 5, 0),
('LAB_302', 'HALLWAY_A', 5, 0),
('HALLWAY_A', 'LIBRARY_2F', 15, 1),
('LIBRARY_2F', 'HALLWAY_A', 15, 1),
('ENTRANCE_A', 'CANTEEN', 25, 0),
('CANTEEN', 'ENTRANCE_A', 25, 0);

-- Insert sample resources
INSERT INTO Resources (NodeID, Type, Status) VALUES
('LAB_302', 'PC_01', 'Available'),
('LAB_302', 'PC_02', 'Available'),
('LIBRARY_2F', 'StudyTable_01', 'Available');
GO
